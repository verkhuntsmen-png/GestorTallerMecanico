package com.refaccionaria.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Configurable lifecycle catalog shared by customers and vehicles. */
@Entity
@Table(name = "estatus", uniqueConstraints = @UniqueConstraint(name = "uk_estatus_valor", columnNames = "strValor"))
public class Estatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 60) @Column(nullable = false, length = 60)
    private String strValor;
    @Size(max = 180) @Column(length = 180)
    private String strDescripcion;
    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() { return id; }
    public String getStrValor() { return strValor; }
    public void setStrValor(String strValor) { this.strValor = strValor; }
    public String getStrDescripcion() { return strDescripcion; }
    public void setStrDescripcion(String strDescripcion) { this.strDescripcion = strDescripcion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
