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

/** Vehicle registered to an active customer. VIN is immutable after registration. */
@Entity
@Table(name = "vehiculo", uniqueConstraints = @UniqueConstraint(name = "uk_vehiculo_vin", columnNames = "vin"))
public class Vehiculo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 17) private String vin;
    @Column(nullable = false, length = 12) private String placa;
    @ManyToOne(optional = false) @JoinColumn(name = "marca_id", nullable = false) private Marca marca;
    @ManyToOne(optional = false) @JoinColumn(name = "modelo_id", nullable = false) private Modelo modelo;
    @Column(nullable = false) private Integer anio;
    @Column(length = 120) private String version;
    @Column(length = 60) private String color;
    @Column(length = 60) private String numeroSerie;
    private Integer kilometraje;
    @Column(length = 60) private String tipoCombustible;
    @Column(length = 30) private String numeroCilindros;
    @Column(length = 60) private String tipoTransmision;
    @ManyToOne(optional = false) @JoinColumn(name = "cliente_id", nullable = false) private Customer cliente;
    @ManyToOne(optional = false) @JoinColumn(name = "status_id", nullable = false) private Estatus estatus;
    public Long getId() { return id; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }
    public Modelo getModelo() { return modelo; }
    public void setModelo(Modelo modelo) { this.modelo = modelo; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }
    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String tipoCombustible) { this.tipoCombustible = tipoCombustible; }
    public String getNumeroCilindros() { return numeroCilindros; }
    public void setNumeroCilindros(String numeroCilindros) { this.numeroCilindros = numeroCilindros; }
    public String getTipoTransmision() { return tipoTransmision; }
    public void setTipoTransmision(String tipoTransmision) { this.tipoTransmision = tipoTransmision; }
    public Customer getCliente() { return cliente; }
    public void setCliente(Customer cliente) { this.cliente = cliente; }
    public Estatus getEstatus() { return estatus; }
    public void setEstatus(Estatus estatus) { this.estatus = estatus; }
}
