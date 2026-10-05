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
        Instant createdAt,
        String createdBy
) {
}
