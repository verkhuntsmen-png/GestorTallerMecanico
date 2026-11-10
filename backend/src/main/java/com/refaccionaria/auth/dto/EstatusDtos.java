package com.refaccionaria.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class EstatusDtos {
    private EstatusDtos() { }
    public record Request(@NotBlank @Size(max = 60) String strValor, @Size(max = 180) String strDescripcion) { }
    public record Response(Long id, String strValor, String strDescripcion, boolean activo) { }
}
