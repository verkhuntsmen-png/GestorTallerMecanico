package com.refaccionaria.auth.dto;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerResponse(
        Long id,
        String fullName,
        String alternateFullName,
        String personalPhone,
        String workPhone,
        Integer age,
        LocalDate birthDate,
        String email,
        String workEmail,
        String street,
        String neighborhood,
        String municipality,
        String state,
        String postalCode,
        String rfc,
        String curp,
        Long tallerId,
        String tallerNombre,
        String status,
        Instant createdAt,
        String createdBy
) {
}
