package com.refaccionaria.auth.controller;

import com.refaccionaria.auth.dto.CustomerResponse;
import com.refaccionaria.auth.dto.CustomerUpdateRequest;
import com.refaccionaria.auth.service.CustomerFacade;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Implements UC-CV-02: filtered customer administration and logical suspension. */
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final CustomerFacade customerFacade;

    public ClienteController(CustomerFacade customerFacade) { this.customerFacade = customerFacade; }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CLIENTS_READ') or hasAuthority('PERM_CLIENTS_WRITE')")
    public Page<CustomerResponse> list(
            @RequestParam(required = false) Long tallerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return customerFacade.list(tallerId, page, size, sortBy, sortDir);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CLIENTS_WRITE')")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return customerFacade.update(id, request, isAdmin);
    }

    @PutMapping("/{id}/suspender")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(customerFacade.suspend(id));
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(customerFacade.activate(id));
    }

    @PutMapping("/{id}/estatus")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse updateStatus(@PathVariable Long id, @RequestParam Long statusId) {
        return customerFacade.updateStatus(id, statusId);
    }
}
