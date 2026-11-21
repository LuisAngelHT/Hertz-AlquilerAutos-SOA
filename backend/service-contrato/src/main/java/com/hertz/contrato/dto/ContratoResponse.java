package com.hertz.contrato.dto;

import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.EstadoContrato;

import java.math.BigDecimal;
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
        Instant actualizadoEn,
        /** Contrato firmado y escaneado; null mientras no se suba. */
        DocumentoResponse documento,
        /** Revision al entregar el vehiculo; null en contratos anteriores a esta funcion. */
        InspeccionResponse entrega,
        /** Revision al recibirlo; null mientras el contrato siga vigente. */
        InspeccionResponse devolucion,
        /** Kilometros recorridos durante el alquiler (null si falta alguna de las dos revisiones). */
        Integer kilometrosRecorridos,
        /** Suma del costo estimado de los danos NUEVOS y de lo que falta o vuelve danado: lo que asume el cliente. null hasta la devolucion. */
        BigDecimal totalDanosACargoCliente) {

    public static ContratoResponse de(Contrato contrato) {
        return de(contrato, null, null, null);
    }

    public static ContratoResponse de(Contrato contrato, DocumentoResponse documento,
                                      InspeccionResponse entrega, InspeccionResponse devolucion) {
        Integer recorridos = entrega != null && devolucion != null
                ? devolucion.kilometraje() - entrega.kilometraje()
                : null;
        // Lo que asume el cliente: los danos nuevos y los elementos del inventario que faltan o vuelven danados.
        BigDecimal total = devolucion == null
                ? null
                : java.util.stream.Stream.concat(
                                devolucion.danos().stream().map(DanoResponse::costoEstimado),
                                devolucion.inventario().stream().map(ItemInventarioResponse::costoEstimado))
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
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
                contrato.getActualizadoEn(),
                documento,
                entrega,
                devolucion,
                recorridos,
                total);
    }
}
