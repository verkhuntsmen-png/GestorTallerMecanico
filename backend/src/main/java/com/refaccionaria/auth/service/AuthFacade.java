package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.AuthDtos.AuthResponse;
import com.refaccionaria.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.refaccionaria.auth.dto.AuthDtos.LoginRequest;
import com.refaccionaria.auth.dto.AuthDtos.RegisterRequest;
import com.refaccionaria.auth.dto.AuthDtos.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/** Facade used by authentication endpoints to keep controllers free of service orchestration details. */
@Service
public class AuthFacade {
    private final AuthService authService;

    public AuthFacade(AuthService authService) {
        this.authService = authService;
    }

    public AuthResponse register(RegisterRequest request) { return authService.register(request); }
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request, servletRequest);
    }
    public Object me(String email) { return authService.me(email); }
    public Object forgotPassword(ForgotPasswordRequest request) { return authService.forgotPassword(request); }
    public Object resetPassword(ResetPasswordRequest request) { return authService.resetPassword(request); }
}
