package com.refaccionaria.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refaccionaria.auth.model.Vehiculo;
import com.refaccionaria.auth.model.VehiculoAuditoria;
import com.refaccionaria.auth.repository.VehiculoAuditoriaRepository;
import org.springframework.stereotype.Service;

/** Persists immutable before/after snapshots for vehicle changes. */
@Service
public class VehiculoAuditoriaService {
    private final VehiculoAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    public VehiculoAuditoriaService(VehiculoAuditoriaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Snapshot snapshot(Vehiculo vehicle) {
        return new Snapshot(vehicle.getVin(), vehicle.getPlaca(), vehicle.getMarca().getNombre(),
                vehicle.getModelo().getNombre(), vehicle.getAnio(), vehicle.getVersion(), vehicle.getColor(),
                vehicle.getNumeroSerie(), vehicle.getKilometraje(), vehicle.getTipoCombustible(),
                vehicle.getNumeroCilindros(), vehicle.getTipoTransmision(), vehicle.getCliente().getId(),
                vehicle.getEstatus().getStrValor());
    }

    public void register(Vehiculo vehicle, String actorEmail, String action, Snapshot before, Snapshot after) {
        VehiculoAuditoria audit = new VehiculoAuditoria();
        audit.setVehiculo(vehicle);
        audit.setAccion(action);
        audit.setActorEmail(actorEmail);
        audit.setDatosAntes(toJson(before));
        audit.setDatosDespues(toJson(after));
        repository.save(audit);
    }

    private String toJson(Snapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible registrar la auditoria del vehiculo", exception);
        }
    }

    public record Snapshot(
            String vin, String placa, String marca, String modelo, Integer anio, String version, String color,
            String numeroSerie, Integer kilometraje, String tipoCombustible, String numeroCilindros,
            String tipoTransmision, Long clienteId, String estatus
    ) { }
}
