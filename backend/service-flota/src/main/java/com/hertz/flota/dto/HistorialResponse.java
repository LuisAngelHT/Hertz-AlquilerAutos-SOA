package com.hertz.flota.dto;

import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.HistorialVehiculo;

import java.time.Instant;

/** Un cambio de estado del historial, tal como lo devuelve la API. */
public record HistorialResponse(String placa, EstadoVehiculo estadoAnterior, EstadoVehiculo estadoNuevo,
                                String usuario, String rol, String motivo, Instant fecha) {

    public static HistorialResponse de(HistorialVehiculo h) {
        return new HistorialResponse(h.getPlaca(), h.getEstadoAnterior(), h.getEstadoNuevo(),
                h.getUsuario(), h.getRol(), h.getMotivo(), h.getFecha());
    }
}
