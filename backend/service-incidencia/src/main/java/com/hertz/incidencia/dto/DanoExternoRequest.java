package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.GravedadDano;
import com.hertz.incidencia.entity.TipoDano;
import com.hertz.incidencia.entity.VistaVehiculo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DanoExternoRequest(
        @NotNull(message = "La vista del dano es obligatoria") VistaVehiculo vista,
        @NotNull(message = "La posicion del dano es obligatoria")
        @DecimalMin(value = "0", message = "La posicion del dano no es valida") @DecimalMax(value = "100", message = "La posicion del dano no es valida")
        @Digits(integer = 3, fraction = 2, message = "La posicion del dano no es valida") BigDecimal posX,
        @NotNull(message = "La posicion del dano es obligatoria")
        @DecimalMin(value = "0", message = "La posicion del dano no es valida") @DecimalMax(value = "100", message = "La posicion del dano no es valida")
        @Digits(integer = 3, fraction = 2, message = "La posicion del dano no es valida") BigDecimal posY,
        @NotNull(message = "El tipo de dano es obligatorio") TipoDano tipo,
        @NotBlank(message = "Describe el dano") @Size(max = 300, message = "La descripcion del dano no puede superar 300 caracteres") String descripcion,
        @NotNull(message = "La gravedad del dano es obligatoria") GravedadDano gravedad,
        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 2 decimales") BigDecimal costoEstimado) {
}
