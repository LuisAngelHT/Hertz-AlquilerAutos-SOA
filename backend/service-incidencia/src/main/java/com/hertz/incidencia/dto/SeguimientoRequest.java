package com.hertz.incidencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeguimientoRequest(
        @NotBlank(message = "Escribe la nota de avance") @Size(max = 500, message = "La nota no puede superar 500 caracteres") String nota) {
}
