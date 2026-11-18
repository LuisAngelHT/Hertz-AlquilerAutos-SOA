package com.hertz.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Pedido de ServicioReserva: apartar un vehiculo de la categoria y sucursal indicadas en esas fechas. */
public record BloqueoRequest(
        @NotBlank(message = "sucursal es obligatoria") @Size(max = 40) String sucursal,
        @NotBlank(message = "categoria es obligatoria") @Size(max = 20) String categoria,
        @NotNull(message = "fechaInicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "fechaFin es obligatoria") LocalDate fechaFin,
        @NotBlank(message = "referencia es obligatoria") @Size(max = 50) String referencia,
        // Opcional: el vehículo concreto que eligió el cliente. Sin placa se aparta cualquiera de la categoría.
        @Size(max = 10) String placa) {
}
