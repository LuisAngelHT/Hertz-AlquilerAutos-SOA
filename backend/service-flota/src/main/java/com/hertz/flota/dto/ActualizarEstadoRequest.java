package com.hertz.flota.dto;

import com.hertz.flota.entity.EstadoVehiculo;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del PUT que cambia el estado: {"estado": "ALQUILADO"}.
 * Si falta o no es un valor válido del enum, la API responde 400 SOLICITUD_INVALIDA.
 */
public record ActualizarEstadoRequest(
        @NotNull(message = "El estado es obligatorio (DISPONIBLE, ALQUILADO o MANTENIMIENTO)")
        EstadoVehiculo estado) {
}
