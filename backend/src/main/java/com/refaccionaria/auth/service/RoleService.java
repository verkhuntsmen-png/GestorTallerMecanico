package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.AuthDtos.RoleRequest;
import com.refaccionaria.auth.dto.AuthDtos.RoleResponse;
import com.refaccionaria.auth.model.Role;
import com.refaccionaria.auth.repository.RoleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final AuthMapper mapper;

    public RoleService(RoleRepository roleRepository, AuthMapper mapper) {
        this.roleRepository = roleRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(mapper::toRoleResponse)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        String name = normalizeRoleName(request.name());
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe un rol con ese nombre");
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(request.description());
        role.setPermissions(normalizePermissions(request));
        return mapper.toRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
        role.setName(normalizeRoleName(request.name()));
        role.setDescription(request.description());
        role.setPermissions(normalizePermissions(request));
        return mapper.toRoleResponse(role);
    }

    private String normalizeRoleName(String name) {
        return name.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
    }

    private java.util.Set<String> normalizePermissions(RoleRequest request) {
        return request.permissions().stream()
                .map(permission -> permission.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
