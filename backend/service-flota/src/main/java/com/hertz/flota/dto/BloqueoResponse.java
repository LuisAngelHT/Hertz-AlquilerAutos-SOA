package com.hertz.flota.dto;

import com.hertz.flota.entity.BloqueoVehiculo;

import java.time.LocalDate;

/** El vehiculo que quedo apartado para una referencia (reserva) y sus fechas. */
public record BloqueoResponse(String referencia, String placa, String sucursal, String categoria,
                              LocalDate fechaInicio, LocalDate fechaFin) {

    public static BloqueoResponse de(BloqueoVehiculo b, String sucursal, String categoria) {
        return new BloqueoResponse(b.getReferencia(), b.getPlaca(), sucursal, categoria,
                b.getFechaInicio(), b.getFechaFin());
    }
}
