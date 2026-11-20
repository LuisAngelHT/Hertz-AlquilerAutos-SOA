package com.hertz.reserva.dto;

import com.hertz.reserva.entity.EstadoReserva;
import com.hertz.reserva.entity.Reserva;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservaResponse(
        UUID idReserva,
        EstadoReserva estado,
        UUID idCliente,
        String categoria,
        String sucursalRecojo,
        String sucursalDevolucion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String placaAsignada,
        String clienteNombre,
        String clienteDocumento,
        String clienteTelefono,
        Instant creadoEn) {

    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getEstado(), r.getIdCliente(), r.getCategoria(),
                r.getSucursalRecojo(), r.getSucursalDevolucion(), r.getFechaInicio(), r.getFechaFin(),
                r.getPlacaAsignada(), r.getClienteNombre(), r.getClienteDocumento(), r.getClienteTelefono(),
                r.getCreadoEn());
    }
}
