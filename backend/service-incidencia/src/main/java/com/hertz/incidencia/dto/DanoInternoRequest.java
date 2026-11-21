package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.GravedadDano;
import com.hertz.incidencia.entity.SistemaVehiculo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DanoInternoRequest(
        @NotNull(message = "El sistema del dano interno es obligatorio") SistemaVehiculo sistema,
        @NotBlank(message = "Describe el dano interno") @Size(max = 300, message = "La descripcion del dano no puede superar 300 caracteres") String descripcion,
        @NotNull(message = "La gravedad del dano es obligatoria") GravedadDano gravedad,
        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 2 decimales") BigDecimal costoEstimado) {
}
