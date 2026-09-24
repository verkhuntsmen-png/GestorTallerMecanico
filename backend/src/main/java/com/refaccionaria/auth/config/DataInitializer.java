package com.refaccionaria.auth.config;

import com.refaccionaria.auth.model.AppUser;
import com.refaccionaria.auth.model.Role;
import com.refaccionaria.auth.repository.RoleRepository;
import com.refaccionaria.auth.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(RoleRepository roles, UserRepository users, PasswordEncoder encoder) {
        return args -> {
            Role admin = ensureRole(roles, "ADMINISTRADOR", "Control total del sistema",
                    Set.of("SECURITY_MANAGE_ROLES", "USERS_READ", "USERS_WRITE", "ORDERS_READ", "ORDERS_WRITE"));
            ensureRole(roles, "CLIENTE", "Consulta sus ordenes y solicitudes",
                    Set.of("ORDERS_READ_OWN", "PROFILE_WRITE"));
            ensureRole(roles, "MECANICO", "Gestion tecnica de reparaciones",
                    Set.of("ORDERS_READ", "ORDERS_WRITE", "DIAGNOSTICS_WRITE"));
            ensureRole(roles, "GERENTE", "Supervision operativa del taller",
                    Set.of("USERS_READ", "ORDERS_READ", "REPORTS_READ"));

            if (!users.existsByEmailIgnoreCase("admin@taller.local")) {
                AppUser user = new AppUser();
                user.setEmail("admin@taller.local");
                user.setFullName("Administrador Taller");
                user.setPasswordHash(encoder.encode("Admin123!Seguro"));
                user.setRoles(Set.of(admin));
                users.save(user);
            }
        };
    }

    private Role ensureRole(RoleRepository roles, String name, String description, Set<String> permissions) {
        return roles.findByNameIgnoreCase(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setPermissions(new LinkedHashSet<>(permissions));
            return roles.save(role);
        });
    }
}
