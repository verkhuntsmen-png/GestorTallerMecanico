package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.AuthDtos.AuthResponse;
import com.refaccionaria.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.refaccionaria.auth.dto.AuthDtos.LoginRequest;
import com.refaccionaria.auth.dto.AuthDtos.MessageResponse;
import com.refaccionaria.auth.dto.AuthDtos.PasswordResetDevResponse;
import com.refaccionaria.auth.dto.AuthDtos.RegisterRequest;
import com.refaccionaria.auth.dto.AuthDtos.ResetPasswordRequest;
import com.refaccionaria.auth.model.AppUser;
import com.refaccionaria.auth.model.PasswordResetToken;
import com.refaccionaria.auth.repository.PasswordResetTokenRepository;
import com.refaccionaria.auth.repository.RoleRepository;
import com.refaccionaria.auth.repository.UserRepository;
import com.refaccionaria.auth.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService attemptService;
    private final TokenHashService tokenHashService;
    private final AuthMapper mapper;
    private final boolean exposePasswordResetToken;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            LoginAttemptService attemptService,
            TokenHashService tokenHashService,
            AuthMapper mapper,
            @Value("${app.security.expose-password-reset-token:false}") boolean exposePasswordResetToken
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.attemptService = attemptService;
        this.tokenHashService = tokenHashService;
        this.mapper = mapper;
        this.exposePasswordResetToken = exposePasswordResetToken;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("El correo ya esta registrado");
        }

        var role = roleRepository.findByNameIgnoreCase("CLIENTE")
                .orElseThrow(() -> new IllegalStateException("Rol CLIENTE no encontrado"));
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(role));
        AppUser saved = userRepository.save(user);
        return new AuthResponse(jwtService.createToken(saved.getEmail()), mapper.toUserResponse(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String email = request.email().trim().toLowerCase();
        String key = email + "|" + clientIp(servletRequest);
        if (attemptService.isBlocked(key)) {
            throw new BadCredentialsException("Demasiados intentos fallidos. Intenta de nuevo en 15 minutos");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
            attemptService.loginSucceeded(key);
            var user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));
            return new AuthResponse(jwtService.createToken(user.getEmail()), mapper.toUserResponse(user));
        } catch (BadCredentialsException ex) {
            attemptService.loginFailed(key);
            throw new BadCredentialsException("Credenciales invalidas");
        }
    }

    @Transactional(readOnly = true)
    public Object me(String email) {
        return mapper.toUserResponse(userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado")));
    }

    @Transactional
    public Object forgotPassword(ForgotPasswordRequest request) {
        var maybeUser = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase());
        if (maybeUser.isEmpty()) {
            return new MessageResponse("Si el correo existe, se genero una liga de recuperacion");
        }

        String rawToken = tokenHashService.newToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(maybeUser.get());
        token.setTokenHash(tokenHashService.sha256(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(30 * 60));
        resetTokenRepository.save(token);

        if (exposePasswordResetToken) {
            return new PasswordResetDevResponse(
                    "Token de recuperacion generado. En produccion se enviaria por correo.",
                    rawToken
            );
        }
        return new MessageResponse("Si el correo existe, se genero una liga de recuperacion");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        var token = resetTokenRepository.findByTokenHash(tokenHashService.sha256(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Token invalido"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expirado o utilizado");
        }

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(Instant.now());
        return new MessageResponse("Contrasena actualizada correctamente");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
