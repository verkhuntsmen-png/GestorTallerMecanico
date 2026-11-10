package com.refaccionaria.auth.config;

import com.refaccionaria.auth.model.AppUser;
import com.refaccionaria.auth.model.Estatus;
import com.refaccionaria.auth.model.Marca;
import com.refaccionaria.auth.model.Modelo;
import com.refaccionaria.auth.model.Role;
import com.refaccionaria.auth.repository.RoleRepository;
import com.refaccionaria.auth.repository.CustomerRepository;
import com.refaccionaria.auth.repository.EstatusRepository;
import com.refaccionaria.auth.repository.MarcaRepository;
import com.refaccionaria.auth.repository.ModeloRepository;
import com.refaccionaria.auth.repository.UserRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(RoleRepository roles, UserRepository users, CustomerRepository customers,
            EstatusRepository statuses, MarcaRepository marcas, ModeloRepository modelos, PasswordEncoder encoder,
            @Value("${app.bootstrap.admin-email:}") String bootstrapAdminEmail,
            @Value("${app.bootstrap.admin-password:}") String bootstrapAdminPassword) {
        return args -> {
            customers.clearLegacyInvalidTallerIds();
            Estatus active = ensureStatus(statuses, "Activo", "Registro disponible para operaciones");
            ensureStatus(statuses, "Suspendido", "Registro temporalmente suspendido");
            ensureStatus(statuses, "Actualizado", "Registro actualizado");
            ensureStatus(statuses, "Eliminado", "Baja logica del registro");
            ensureStatus(statuses, "Cancelado", "Operacion cancelada");
            customers.assignMissingStatusId(active.getId());
            loadVehicleCatalog(marcas, modelos);
            Role admin = ensureRole(roles, "ADMINISTRADOR", "Control total del sistema",
                    Set.of("SECURITY_MANAGE_ROLES", "USERS_READ", "USERS_WRITE", "ORDERS_READ", "ORDERS_WRITE",
                            "CLIENTS_READ", "CLIENTS_WRITE"));
            ensureRole(roles, "RECEPCIONISTA", "Registro y consulta de clientes del taller",
                    Set.of("CLIENTS_READ", "CLIENTS_WRITE"));
            ensureRole(roles, "CLIENTE", "Consulta sus ordenes y solicitudes",
                    Set.of("ORDERS_READ_OWN", "PROFILE_WRITE"));
            ensureRole(roles, "MECANICO", "Gestion tecnica de reparaciones",
                    Set.of("ORDERS_READ", "ORDERS_WRITE", "DIAGNOSTICS_WRITE"));
            ensureRole(roles, "GERENTE", "Supervision operativa del taller",
                    Set.of("USERS_READ", "ORDERS_READ", "REPORTS_READ"));

            if (!bootstrapAdminEmail.isBlank() && !bootstrapAdminPassword.isBlank()
                    && !users.existsByEmailIgnoreCase(bootstrapAdminEmail)) {
                AppUser user = new AppUser();
                user.setEmail(bootstrapAdminEmail);
                user.setFullName("Administrador Taller");
                user.setPasswordHash(encoder.encode(bootstrapAdminPassword));
                user.setRoles(Set.of(admin));
                users.save(user);
            }
        };
    }

    private Estatus ensureStatus(EstatusRepository statuses, String value, String description) {
        return statuses.findByStrValorIgnoreCase(value).orElseGet(() -> {
            Estatus status = new Estatus(); status.setStrValor(value); status.setStrDescripcion(description);
            return statuses.save(status);
        });
    }

    /** Loads the normalized CSV only when the catalog is empty, preserving manual additions. */
    private void loadVehicleCatalog(MarcaRepository marcas, ModeloRepository modelos) {
        if (modelos.count() > 0) return;
        try (var input = getClass().getResourceAsStream("/data/vehiculos_normalizado.csv")) {
            if (input == null) throw new IllegalStateException("No se encontro vehiculos_normalizado.csv");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",", -1); if (row.length < 5) continue;
                    String brandName = row[0].trim().toUpperCase(Locale.ROOT); String modelName = row[1].trim().toUpperCase(Locale.ROOT);
                    if (brandName.isBlank() || modelName.isBlank()) continue;
                    Marca brand = marcas.findByNombreIgnoreCase(brandName).orElseGet(() -> { Marca item = new Marca(); item.setNombre(brandName); return marcas.save(item); });
                    if (modelos.findByMarcaIdAndNombreIgnoreCase(brand.getId(), modelName).isEmpty()) {
                        Modelo model = new Modelo(); model.setMarca(brand); model.setNombre(modelName); model.setTipoCombustible(row[2].trim()); model.setNumeroCilindros(row[3].trim()); model.setTipoTransmision(row[4].trim()); modelos.save(model);
                    }
                }
            }
        } catch (Exception ex) { throw new IllegalStateException("No fue posible cargar el catalogo de vehiculos", ex); }
    }

    private Role ensureRole(RoleRepository roles, String name, String description, Set<String> permissions) {
        return roles.findByNameIgnoreCase(name).map(role -> {
            role.getPermissions().addAll(permissions);
            return roles.save(role);
        }).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setPermissions(new LinkedHashSet<>(permissions));
            return roles.save(role);
        });
    }
}
