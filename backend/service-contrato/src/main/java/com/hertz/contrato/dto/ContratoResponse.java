package com.hertz.contrato.dto;

import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.EstadoContrato;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoResponse(
        UUID idContrato,
        UUID idReserva,
        UUID idCliente,
        String placa,
        String sucursalRecojo,
        String sucursalDevolucion,
        LocalDate fechaInicio,
        LocalDate fechaFinPrevista,
        LocalDate fechaDevolucion,
        EstadoContrato estado,
        String registradoPor,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static ContratoResponse de(Contrato contrato) {
        return new ContratoResponse(
                contrato.getId(),
                contrato.getIdReserva(),
                contrato.getIdCliente(),
                contrato.getPlaca(),
                contrato.getSucursalRecojo(),
                contrato.getSucursalDevolucion(),
                contrato.getFechaInicio(),
                contrato.getFechaFinPrevista(),
                contrato.getFechaDevolucion(),
                contrato.getEstado(),
                contrato.getRegistradoPor(),
                contrato.getCreadoEn(),
                contrato.getActualizadoEn());
    }
}
