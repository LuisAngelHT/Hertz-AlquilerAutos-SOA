package com.hertz.reserva.dto;

import com.hertz.reserva.entity.EstadoReserva;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * modificarReserva: o se cambian fechas y/o categoría (solo CONFIRMADA), o se cambia el estado
 * (por ejemplo EN_CURSO en el check-out), pero no las dos cosas a la vez. Los campos que no se
 * envían se conservan. "motivo" es opcional y queda en el historial.
 */
public record ModificarReservaRequest(
        @Size(max = 20) String categoria,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoReserva estado,
        @Size(max = 200, message = "el motivo no puede superar 200 caracteres") String motivo) {
}
