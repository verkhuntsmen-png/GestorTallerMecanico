package com.refaccionaria.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** JSON contract for editing customer information while preserving the stored photo. */
public record CustomerUpdateRequest(
        @NotBlank @Size(max = 160) @Pattern(regexp = "^[\\p{L}]+(?:[\\p{L} ]*[\\p{L}])?$") String fullName,
        @NotBlank @Size(max = 160) @Pattern(regexp = "^[\\p{L}]+(?:[\\p{L} ]*[\\p{L}])?$") String alternateFullName,
        @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$") String personalPhone,
        @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$") String workPhone,
        @NotNull @Min(0) @Max(130) Integer age,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Email @Size(max = 160) String email,
        @Email @Size(max = 160) String workEmail,
        @NotBlank @Size(max = 180) @Pattern(regexp = "^[\\p{L}0-9 .,#-]+$") String street,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[\\p{L}0-9 .-]+$") String neighborhood,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[\\p{L}0-9 .-]+$") String municipality,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[\\p{L}0-9 .-]+$") String state,
        @NotBlank @Pattern(regexp = "^\\d{5}$") String postalCode,
        @Pattern(regexp = CustomerRegistrationRequest.RFC, message = "RFC mexicano invalido") String rfc,
        @NotBlank @Pattern(regexp = CustomerRegistrationRequest.CURP, message = "CURP mexicana invalida") String curp,
        @NotNull Long tallerId
) { }
