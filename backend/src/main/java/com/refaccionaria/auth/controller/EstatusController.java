package com.refaccionaria.auth.controller;

import com.refaccionaria.auth.dto.EstatusDtos.Request;
import com.refaccionaria.auth.dto.EstatusDtos.Response;
import com.refaccionaria.auth.service.EstatusFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only CRUD for customer and vehicle lifecycle statuses. */
@RestController
@RequestMapping("/api/v1/estatus")
@PreAuthorize("hasRole('ADMIN')")
public class EstatusController {
    private final EstatusFacade facade;
    public EstatusController(EstatusFacade facade) { this.facade = facade; }
    @GetMapping public List<Response> list() { return facade.list(); }
    @PostMapping public ResponseEntity<Response> create(@Valid @RequestBody Request request) { return ResponseEntity.status(201).body(facade.create(request)); }
    @PutMapping("/{id}") public Response update(@PathVariable Long id, @Valid @RequestBody Request request) { return facade.update(id, request); }
    @DeleteMapping("/{id}") public Response deactivate(@PathVariable Long id) { return facade.deactivate(id); }
}
