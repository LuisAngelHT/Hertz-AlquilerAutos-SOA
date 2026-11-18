package com.hertz.reserva.dto;

import com.hertz.reserva.entity.EstadoReserva;
import com.hertz.reserva.entity.HistorialReserva;

import java.time.Instant;

public record HistorialReservaResponse(String accion, EstadoReserva estadoAnterior, EstadoReserva estadoNuevo,
                                       String usuario, String rol, String detalle, Instant fecha) {

    public static HistorialReservaResponse de(HistorialReserva h) {
        return new HistorialReservaResponse(h.getAccion(), h.getEstadoAnterior(), h.getEstadoNuevo(),
                h.getUsuario(), h.getRol(), h.getDetalle(), h.getFecha());
    }
}
