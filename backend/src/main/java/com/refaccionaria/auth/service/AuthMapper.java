package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.AuthDtos.RoleResponse;
import com.refaccionaria.auth.dto.AuthDtos.UserResponse;
import com.refaccionaria.auth.model.AppUser;
import com.refaccionaria.auth.model.Role;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {
    public RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.getPermissions());
    }

    public UserResponse toUserResponse(AppUser user) {
        var roles = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(this::toRoleResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), roles, user.getCreatedAt());
    }
}
