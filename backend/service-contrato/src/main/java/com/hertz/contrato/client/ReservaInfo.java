package com.hertz.contrato.client;

import java.time.LocalDate;
import java.util.UUID;

public record ReservaInfo(
        UUID idReserva,
        String estado,
        UUID idCliente,
        String sucursalRecojo,
        String sucursalDevolucion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String placaAsignada) {
}
