package com.refaccionaria.auth.controller;

import com.refaccionaria.auth.dto.VehiculoDtos.BrandResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.DetailResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.ModelResponse;
import com.refaccionaria.auth.dto.VehiculoDtos.Request;
import com.refaccionaria.auth.dto.VehiculoDtos.Response;
import com.refaccionaria.auth.dto.VehiculoDtos.StatusRequest;
import com.refaccionaria.auth.dto.VehiculoDtos.UpdateRequest;
import com.refaccionaria.auth.service.VehiculoFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vehicle registration, lookup, update and lifecycle endpoints. */
@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {
    private final VehiculoFacade facade;
    public VehiculoController(VehiculoFacade facade) { this.facade = facade; }
    @GetMapping("/marcas") @PreAuthorize("hasAuthority('PERM_CLIENTS_READ') or hasAuthority('PERM_CLIENTS_WRITE')")
    public List<BrandResponse> brands() { return facade.brands(); }
    @GetMapping("/marcas/{marcaId}/modelos") @PreAuthorize("hasAuthority('PERM_CLIENTS_READ') or hasAuthority('PERM_CLIENTS_WRITE')")
    public List<ModelResponse> models(@PathVariable Long marcaId) { return facade.models(marcaId); }
    @GetMapping("/ficha")
    @PreAuthorize("hasAuthority('PERM_CLIENTS_READ') or hasAuthority('PERM_CLIENTS_WRITE')")
    public DetailResponse detail(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String vin,
            @RequestParam(required = false) String placa) {
        return facade.detail(id, vin, placa);
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) { return ResponseEntity.status(201).body(facade.create(request)); }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPCIONISTA')")
    public Response update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request,
            org.springframework.security.core.Authentication authentication) {
        return facade.update(id, request, authentication.getName());
    }
    @PutMapping("/{id}/estatus")
    @PreAuthorize("hasRole('ADMIN')")
    public Response updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
            org.springframework.security.core.Authentication authentication) {
        return facade.updateStatus(id, request, authentication.getName());
    }
}
