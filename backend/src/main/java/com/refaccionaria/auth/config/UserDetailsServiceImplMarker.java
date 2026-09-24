package com.refaccionaria.auth.config;

import com.refaccionaria.auth.service.UserDetailsServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceImplMarker {
    private final UserDetailsServiceImpl userDetailsService;

    public UserDetailsServiceImplMarker(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public UserDetailsServiceImpl userDetailsService() {
        return userDetailsService;
    }
}
