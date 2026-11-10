package com.refaccionaria.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/** Workshop owned by the company and selectable when registering a customer. */
@Entity
@Table(name = "talleres", uniqueConstraints = {
        @UniqueConstraint(name = "uk_taller_rfc", columnNames = "rfc"),
        @UniqueConstraint(name = "uk_taller_razon_social", columnNames = "razonSocial")
})
public class Taller {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 160) private String nombreTaller;
    @NotBlank @Column(nullable = false, length = 180) private String razonSocial;
    @NotBlank @Pattern(regexp = "^([A-ZÑ&]{3,4})([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])([A-Z0-9]{3})$")
    @Column(nullable = false, length = 13) private String rfc;
    @NotBlank @Email @Column(nullable = false, length = 160) private String emailContacto;
    @Column(nullable = false, length = 16) private String telefono;
    @Column(nullable = false, length = 180) private String calle;
    @Column(nullable = false, length = 120) private String municipio;
    @Column(nullable = false, length = 120) private String estado;
    @Column(nullable = false, length = 5) private String codigoPostal;
    @Column(nullable = false, length = 120) private String colonia;
    @Column(nullable = false, length = 120) private String fotoUrl;
    @Column(nullable = false) private boolean activo = true;
    @Column(nullable = false, updatable = false) private Instant creadoEn = Instant.now();

    public Long getId() { return id; }
    public String getNombreTaller() { return nombreTaller; }
    public void setNombreTaller(String nombreTaller) { this.nombreTaller = nombreTaller; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public String getColonia() { return colonia; }
    public void setColonia(String colonia) { this.colonia = colonia; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getCreadoEn() { return creadoEn; }
}
