package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.entity.TipoIncidencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncidenciaResponse(
        UUID idIncidencia,
        String idVehiculo,
        String sucursal,
        UUID idContrato,
        TipoIncidencia tipo,
        String descripcion,
        BigDecimal costoEstimado,
        EstadoIncidencia estado,
        String registradaPor,
        Instant creadoEn,
        /** Cuando se espera que el vehiculo salga del taller (null si el mecanico aun no estimo los dias). */
        LocalDate fechaSalidaPrevista,
        /** El mecanico ya empezo a llenar la ficha del taller. */
        boolean tieneFicha) {

    public IncidenciaResponse(UUID idIncidencia, String idVehiculo, String sucursal, UUID idContrato,
                              TipoIncidencia tipo, String descripcion, BigDecimal costoEstimado,
                              EstadoIncidencia estado, String registradaPor, Instant creadoEn) {
        this(idIncidencia, idVehiculo, sucursal, idContrato, tipo, descripcion, costoEstimado, estado,
                registradaPor, creadoEn, null, false);
    }

    public IncidenciaResponse conFicha(LocalDate fechaSalidaPrevista) {
        return new IncidenciaResponse(idIncidencia, idVehiculo, sucursal, idContrato, tipo, descripcion,
                costoEstimado, estado, registradaPor, creadoEn, fechaSalidaPrevista, true);
    }
}
