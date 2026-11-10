package com.refaccionaria.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.refaccionaria.auth.model.AppUser;
import com.refaccionaria.auth.repository.CustomerRepository;
import com.refaccionaria.auth.repository.EstatusRepository;
import com.refaccionaria.auth.repository.MarcaRepository;
import com.refaccionaria.auth.repository.ModeloRepository;
import com.refaccionaria.auth.repository.RoleRepository;
import com.refaccionaria.auth.repository.UserRepository;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

class DataInitializerTest {
    @Test
    void doesNotCreateAdministratorWithoutBootstrapCredentials() throws Exception {
        List<AppUser> savedUsers = new ArrayList<>();

        runner(savedUsers, "", "").run();

        assertEquals(List.of(), savedUsers);
    }

    @Test
    void createsAdministratorOnlyFromExternalBootstrapCredentials() throws Exception {
        List<AppUser> savedUsers = new ArrayList<>();

        runner(savedUsers, "admin@example.test", "UnaContrasenaSegura1!").run();

        assertEquals(1, savedUsers.size());
        AppUser savedUser = savedUsers.getFirst();
        assertEquals("admin@example.test", savedUser.getEmail());
        assertEquals("hash-seguro", savedUser.getPasswordHash());
        assertNotEquals("UnaContrasenaSegura1!", savedUser.getPasswordHash());
    }

    private CommandLineRunner runner(List<AppUser> savedUsers, String email, String password) {
        UserRepository users = repository(UserRepository.class, (method, arguments) -> {
            if ("existsByEmailIgnoreCase".equals(method)) return false;
            if ("save".equals(method)) {
                savedUsers.add((AppUser) arguments[0]);
                return arguments[0];
            }
            return null;
        });
        RoleRepository roles = repository(RoleRepository.class, (method, arguments) -> {
            if ("findByNameIgnoreCase".equals(method)) return Optional.empty();
            if ("save".equals(method)) return arguments[0];
            return null;
        });
        EstatusRepository statuses = repository(EstatusRepository.class, (method, arguments) -> {
            if ("findByStrValorIgnoreCase".equals(method)) return Optional.empty();
            if ("save".equals(method)) return arguments[0];
            return null;
        });
        ModeloRepository modelos = repository(ModeloRepository.class,
                (method, arguments) -> "count".equals(method) ? 1L : null);
        PasswordEncoder encoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "hash-seguro";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };
        return new DataInitializer().seedData(roles, users,
                repository(CustomerRepository.class, (method, arguments) -> null), statuses,
                repository(MarcaRepository.class, (method, arguments) -> null), modelos, encoder, email, password);
    }

    @SuppressWarnings("unchecked")
    private <T> T repository(Class<T> type, RepositoryInvocation invocation) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {type},
                (proxy, method, arguments) -> invocation.call(method.getName(), arguments));
    }

    @FunctionalInterface
    private interface RepositoryInvocation {
        Object call(String method, Object[] arguments);
    }
}
