package com.refaccionaria.auth.dto;

import java.time.Instant;

/** Safe workshop projection exposed to the frontend. */
public record TallerResponse(Long id, String nombreTaller, String razonSocial, String rfc, String emailContacto,
        String telefono, String calle, String municipio, String estado, String codigoPostal, String colonia,
        String fotoUrl, boolean activo, Instant creadoEn) { }
