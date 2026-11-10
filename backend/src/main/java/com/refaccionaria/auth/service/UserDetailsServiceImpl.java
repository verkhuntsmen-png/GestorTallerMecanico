package com.refaccionaria.auth.service;

import com.refaccionaria.auth.repository.UserRepository;
import java.util.ArrayList;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var appUser = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        appUser.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            // Keeps the legacy ADMINISTRADOR role compatible with the standard ROLE_ADMIN policy.
            if ("ADMINISTRADOR".equalsIgnoreCase(role.getName())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            role.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERM_" + permission)));
        });
        return User.withUsername(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .disabled(!appUser.isEnabled())
                .authorities(authorities)
                .build();
    }
}
