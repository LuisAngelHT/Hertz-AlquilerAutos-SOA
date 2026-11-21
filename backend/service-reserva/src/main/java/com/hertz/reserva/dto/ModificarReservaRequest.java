package com.hertz.reserva.dto;

import com.hertz.reserva.entity.EstadoReserva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * modificarReserva: o se cambian fechas y/o categoría (solo CONFIRMADA), o se cambia el estado
 * (por ejemplo EN_CURSO en la entrega), pero no las dos cosas a la vez. Los campos que no se
 * envían se conservan. "motivo" es opcional y queda en el historial.
 *
 * Entregar el vehículo (EN_CURSO) exige "entrega" (la revisión del vehículo al salir) y
 * "contratoFirmado" = true; recibirlo (FINALIZADA) exige "devolucion" (la revisión al volver).
 */
public record ModificarReservaRequest(
        @Size(max = 20) String categoria,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoReserva estado,
        @Size(max = 200, message = "el motivo no puede superar 200 caracteres") String motivo,
        @Valid EntregaDto entrega,
        Boolean contratoFirmado,
        @Valid DevolucionDto devolucion) {

    /** Cambios que no son entrega ni devolución (fechas, categoría, cancelar). */
    public ModificarReservaRequest(String categoria, LocalDate fechaInicio, LocalDate fechaFin,
                                   EstadoReserva estado, String motivo) {
        this(categoria, fechaInicio, fechaFin, estado, motivo, null, null, null);
    }
}
