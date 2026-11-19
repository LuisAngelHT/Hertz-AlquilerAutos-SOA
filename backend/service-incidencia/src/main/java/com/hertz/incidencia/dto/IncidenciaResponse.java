package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.entity.TipoIncidencia;

import java.math.BigDecimal;
import java.time.Instant;
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
        Instant creadoEn) {
}