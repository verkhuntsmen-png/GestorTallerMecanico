package com.refaccionaria.auth.controller;

import com.refaccionaria.auth.dto.TallerRegistrationRequest;
import com.refaccionaria.auth.dto.TallerResponse;
import com.refaccionaria.auth.service.TallerFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST boundary for workshop creation and the active-workshop selector. */
@RestController
@RequestMapping("/api/v1/talleres")
public class TallerController {
    private final TallerFacade tallerFacade;

    public TallerController(TallerFacade tallerFacade) { this.tallerFacade = tallerFacade; }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CLIENTS_READ') or hasAuthority('PERM_CLIENTS_WRITE')")
    public List<TallerResponse> listActive() { return tallerFacade.listActive(); }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TallerResponse> create(@Valid @ModelAttribute TallerRegistrationRequest request) {
        return ResponseEntity.status(201).body(tallerFacade.create(request));
    }
}
