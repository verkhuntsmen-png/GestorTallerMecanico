package com.refaccionaria.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Immutable audit event for vehicle updates and lifecycle changes. */
@Entity
@Table(name = "vehiculo_auditoria")
public class VehiculoAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(nullable = false, length = 160)
    private String actorEmail;

    /**
     * A complete JSON snapshot exceeds the capacity that MySQL assigns to its
     * implicit text LOB mapping. Keep the physical column large enough for
     * before/after audit records.
     */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String datosAntes;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String datosDespues;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }
    public void setAccion(String accion) { this.accion = accion; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public void setDatosAntes(String datosAntes) { this.datosAntes = datosAntes; }
    public void setDatosDespues(String datosDespues) { this.datosDespues = datosDespues; }
}
