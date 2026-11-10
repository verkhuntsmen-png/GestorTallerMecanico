package com.refaccionaria.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class VehiculoDtos {
    private VehiculoDtos() { }
    public record BrandResponse(Long id, String nombre) { }
    public record ModelResponse(Long id, String nombre, String tipoCombustible, String numeroCilindros, String tipoTransmision) { }
    public record Request(
            @NotBlank @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN invalido") String vin,
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{3,4}-?[A-Z0-9]{2,4}$", message = "Placa mexicana invalida") String placa,
            Long marcaId, Long modeloId,
            @Size(max = 100) String nuevaMarca, @Size(max = 140) String nuevoModelo,
            @NotNull @Min(1900) @Max(2100) Integer anio,
            @NotBlank @Size(max = 120) String version, @NotBlank @Size(max = 60) String color,
            @NotBlank @Size(max = 60) String numeroSerie, @NotNull @Min(0) Integer kilometraje,
            @NotBlank @Size(max = 60) String tipoCombustible, @NotBlank @Size(max = 30) String numeroCilindros,
            @NotBlank @Size(max = 60) String tipoTransmision,
            @NotNull Long clienteId
    ) { }
    public record UpdateRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{3,4}-?[A-Z0-9]{2,4}$", message = "Placa mexicana invalida") String placa,
            Long marcaId, Long modeloId,
            @Size(max = 100) String nuevaMarca, @Size(max = 140) String nuevoModelo,
            @NotNull @Min(1900) @Max(2100) Integer anio,
            @NotBlank @Size(max = 120) String version, @NotBlank @Size(max = 60) String color,
            @NotBlank @Size(max = 60) String numeroSerie, @NotNull @Min(0) Integer kilometraje,
            @NotBlank @Size(max = 60) String tipoCombustible, @NotBlank @Size(max = 30) String numeroCilindros,
            @NotBlank @Size(max = 60) String tipoTransmision
    ) { }
    public record StatusRequest(@NotNull Long statusId) { }
    public record Response(Long id, String vin, String placa, Long marcaId, String marca, Long modeloId, String modelo,
            Integer anio, String version, String color, String tipoCombustible, String numeroCilindros,
            String tipoTransmision, Long clienteId, String estatus) { }
    public record CustomerScopeResponse(Long id, String nombreCompleto) { }
    public record WorkshopScopeResponse(Long id, String nombreTaller, String razonSocial, boolean activo) { }
    public record DetailResponse(
            Long id, String vin, String placa, Long marcaId, String marca, Long modeloId, String modelo,
            Integer anio, String version, String color, String numeroSerie, Integer kilometraje,
            String tipoCombustible, String numeroCilindros, String tipoTransmision, String estatus,
            CustomerScopeResponse cliente, WorkshopScopeResponse taller
    ) { }
}
