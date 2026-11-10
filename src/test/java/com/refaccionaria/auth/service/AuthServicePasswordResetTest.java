package com.refaccionaria.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.refaccionaria.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.refaccionaria.auth.dto.AuthDtos.MessageResponse;
import com.refaccionaria.auth.dto.AuthDtos.PasswordResetDevResponse;
import com.refaccionaria.auth.model.AppUser;
import com.refaccionaria.auth.model.PasswordResetToken;
import com.refaccionaria.auth.repository.PasswordResetTokenRepository;
import com.refaccionaria.auth.repository.UserRepository;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthServicePasswordResetTest {
    @Test
    void doesNotExposeResetTokenOutsideTheSecurityLab() {
        List<PasswordResetToken> savedTokens = new ArrayList<>();
        AuthService service = serviceWith(savedTokens, false);

        Object response = service.forgotPassword(new ForgotPasswordRequest("persona@example.test"));

        assertInstanceOf(MessageResponse.class, response);
        assertFalse(savedTokens.isEmpty());
    }

    @Test
    void exposesResetTokenOnlyWhenExplicitlyEnabledForTheSecurityLab() {
        List<PasswordResetToken> savedTokens = new ArrayList<>();
        AuthService service = serviceWith(savedTokens, true);

        PasswordResetDevResponse response = assertInstanceOf(PasswordResetDevResponse.class,
                service.forgotPassword(new ForgotPasswordRequest("persona@example.test")));

        assertNotNull(response.resetToken());
        assertFalse(response.resetToken().isBlank());
        assertFalse(savedTokens.isEmpty());
    }

    private AuthService serviceWith(List<PasswordResetToken> savedTokens, boolean exposeResetToken) {
        AppUser user = new AppUser();
        user.setEmail("persona@example.test");
        UserRepository users = (UserRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {UserRepository.class}, (proxy, method, arguments) -> {
                    if ("findByEmailIgnoreCase".equals(method.getName())) return Optional.of(user);
                    throw new UnsupportedOperationException(method.getName());
                });
        PasswordResetTokenRepository tokens = (PasswordResetTokenRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {PasswordResetTokenRepository.class},
                (proxy, method, arguments) -> {
                    if ("save".equals(method.getName())) {
                        savedTokens.add((PasswordResetToken) arguments[0]);
                        return arguments[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new AuthService(users, null, tokens, null, null, null, null, new TokenHashService(), null,
                exposeResetToken);
    }
}
