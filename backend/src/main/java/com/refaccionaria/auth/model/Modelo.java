package com.refaccionaria.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Vehicle model and technical suggestions imported from the normalized catalog. */
@Entity
@Table(name = "modelo", uniqueConstraints = @UniqueConstraint(name = "uk_modelo_marca", columnNames = {"marca_id", "nombre"}))
public class Modelo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 140) private String nombre;
    @ManyToOne(optional = false) @JoinColumn(name = "marca_id", nullable = false) private Marca marca;
    @Column(length = 60) private String tipoCombustible;
    @Column(length = 30) private String numeroCilindros;
    @Column(length = 60) private String tipoTransmision;
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }
    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String tipoCombustible) { this.tipoCombustible = tipoCombustible; }
    public String getNumeroCilindros() { return numeroCilindros; }
    public void setNumeroCilindros(String numeroCilindros) { this.numeroCilindros = numeroCilindros; }
    public String getTipoTransmision() { return tipoTransmision; }
    public void setTipoTransmision(String tipoTransmision) { this.tipoTransmision = tipoTransmision; }
}
