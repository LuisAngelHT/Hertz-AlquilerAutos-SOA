package com.hertz.incidencia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Al cerrar la incidencia el taller deja constancia de lo que se reparo y de lo que costo. */
public record CierreRequest(
        @NotBlank(message = "Indica que trabajo se realizo") @Size(max = 1000, message = "El trabajo realizado no puede superar 1000 caracteres") String trabajoRealizado,
        @NotNull(message = "Indica el costo final de la reparacion")
        @DecimalMin(value = "0.00", message = "El costo final no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo final admite hasta 2 decimales") BigDecimal costoFinal) {
}
