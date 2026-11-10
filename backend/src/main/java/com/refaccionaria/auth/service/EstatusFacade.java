package com.refaccionaria.auth.service;

import com.refaccionaria.auth.dto.EstatusDtos.Request;
import com.refaccionaria.auth.dto.EstatusDtos.Response;
import com.refaccionaria.auth.model.Estatus;
import com.refaccionaria.auth.repository.EstatusRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administration facade for the shared status catalog. */
@Service
public class EstatusFacade {
    private final EstatusRepository repository;
    public EstatusFacade(EstatusRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Response> list() { return repository.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true) public List<Response> listActive() { return repository.findByActivoTrueOrderByStrValorAsc().stream().map(this::toResponse).toList(); }
    @Transactional public Response create(Request request) {
        String value = clean(request.strValor());
        if (repository.existsByStrValorIgnoreCase(value)) throw new IllegalArgumentException("El estatus ya existe");
        Estatus status = new Estatus(); status.setStrValor(value); status.setStrDescripcion(optional(request.strDescripcion()));
        return toResponse(repository.save(status));
    }
    @Transactional public Response update(Long id, Request request) {
        Estatus status = find(id); String value = clean(request.strValor());
        repository.findByStrValorIgnoreCase(value).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalArgumentException("El estatus ya existe"); });
        status.setStrValor(value); status.setStrDescripcion(optional(request.strDescripcion())); return toResponse(repository.save(status));
    }
    @Transactional public Response deactivate(Long id) { Estatus status = find(id); status.setActivo(false); return toResponse(repository.save(status)); }
    @Transactional(readOnly = true) public Estatus activeByValue(String value) {
        return repository.findByStrValorIgnoreCase(value).filter(Estatus::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("No existe un estatus activo: " + value));
    }
    @Transactional(readOnly = true) public Estatus activeById(Long id) { return repository.findById(id).filter(Estatus::isActivo).orElseThrow(() -> new IllegalArgumentException("Estatus no encontrado o inactivo")); }
    private Estatus find(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Estatus no encontrado")); }
    private Response toResponse(Estatus status) { return new Response(status.getId(), status.getStrValor(), status.getStrDescripcion(), status.isActivo()); }
    private String clean(String value) { return value.trim().replaceAll("\\s+", " "); }
    private String optional(String value) { return value == null || value.isBlank() ? null : clean(value); }
}
