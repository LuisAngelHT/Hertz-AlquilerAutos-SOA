package com.hertz.contrato.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearContratoRequest(
        @NotNull(message = "idReserva es obligatorio") UUID idReserva) {
}
