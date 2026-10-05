package com.refaccionaria.auth.controller;

import com.refaccionaria.auth.dto.CustomerRegistrationRequest;
import com.refaccionaria.auth.service.CustomerFacade;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerFacade customerFacade;

    public CustomerController(CustomerFacade customerFacade) {
        this.customerFacade = customerFacade;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> register(@Valid @ModelAttribute CustomerRegistrationRequest request, Principal principal) {
        return ResponseEntity.status(201).body(customerFacade.register(request, principal.getName()));
    }
}
