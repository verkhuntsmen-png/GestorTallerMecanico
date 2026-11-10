package com.refaccionaria.auth.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

class VehiculoControllerSecurityTest {
    @Test
    void detailEndpointRequiresCustomerReadOrWritePermission() throws NoSuchMethodException {
        Method detail = VehiculoController.class.getMethod("detail", Long.class, String.class, String.class);
        PreAuthorize preAuthorize = detail.getAnnotation(PreAuthorize.class);

        assertTrue(preAuthorize.value().contains("PERM_CLIENTS_READ"));
        assertTrue(preAuthorize.value().contains("PERM_CLIENTS_WRITE"));
    }

    @Test
    void updateAndStatusEndpointsDeclareExpectedRoles() throws NoSuchMethodException {
        Method update = VehiculoController.class.getMethod("update", Long.class,
                com.refaccionaria.auth.dto.VehiculoDtos.UpdateRequest.class, Authentication.class);
        Method updateStatus = VehiculoController.class.getMethod("updateStatus", Long.class,
                com.refaccionaria.auth.dto.VehiculoDtos.StatusRequest.class, Authentication.class);

        assertTrue(update.getAnnotation(PreAuthorize.class).value().contains("RECEPCIONISTA"));
        assertTrue(updateStatus.getAnnotation(PreAuthorize.class).value().contains("hasRole('ADMIN')"));
    }
}
