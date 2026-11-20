package com.hertz.reserva.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * crearReserva. "idCliente" lo envía el personal (el cliente al que se reserva); si quien reserva
 * es un CLIENTE, se ignora y se usa su propio perfil.
 */
public record ReservaRequest(
        UUID idCliente,
        @NotBlank(message = "categoria es obligatoria") @Size(max = 20) String categoria,
        @NotBlank(message = "sucursalRecojo es obligatoria") @Size(max = 40) String sucursalRecojo,
        @NotBlank(message = "sucursalDevolucion es obligatoria") @Size(max = 40) String sucursalDevolucion,
        @NotNull(message = "fechaInicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "fechaFin es obligatoria") LocalDate fechaFin,
        // Opcional: el vehículo concreto que el cliente eligió en el catálogo. Sin placa se asigna uno de la categoría.
        @Size(max = 10) String placa) {
}
