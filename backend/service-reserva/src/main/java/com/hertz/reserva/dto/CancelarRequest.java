package com.hertz.reserva.dto;

import jakarta.validation.constraints.Size;

/** cancelarReserva: el motivo es opcional y queda en el historial. */
public record CancelarRequest(@Size(max = 200, message = "el motivo no puede superar 200 caracteres") String motivo) {
}
