package com.hertz.contrato.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * El contrato se crea al ENTREGAR el vehiculo: junto con la revision de salida y la confirmacion de que el
 * contrato impreso fue firmado por el cliente.
 */
public record CrearContratoRequest(
        @NotNull(message = "idReserva es obligatorio") UUID idReserva,
        @NotNull(message = "La revision del vehiculo al entregarlo es obligatoria") @Valid InspeccionRequest entrega,
        @NotNull(message = "Confirma que el contrato fue firmado") Boolean contratoFirmado) {
}
