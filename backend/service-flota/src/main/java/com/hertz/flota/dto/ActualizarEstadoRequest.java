package com.hertz.flota.dto;

import com.hertz.flota.entity.EstadoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del PUT que cambia el estado: {"estado": "MANTENIMIENTO", "motivo": "Golpe en puerta"}.
 * El motivo es opcional y queda en el historial. Si el estado falta o no es un valor válido
 * del enum, la API responde 400 SOLICITUD_INVALIDA.
 */
public record ActualizarEstadoRequest(
        @NotNull(message = "El estado es obligatorio (DISPONIBLE, ALQUILADO, MANTENIMIENTO o FUERA_DE_SERVICIO)")
        EstadoVehiculo estado,
        @Size(max = 200, message = "El motivo no puede pasar de 200 caracteres")
        String motivo) {
}
