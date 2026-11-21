package com.hertz.reserva.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Un dano o defecto observado en la revision del vehiculo (se reenvia tal cual al servicio de contratos). Si se marco
 * en el dibujo del vehiculo trae su vista y su posicion (0 a 100); si no, esos campos van vacios.
 */
public record DanoDto(
        @NotNull(message = "La zona del dano es obligatoria")
        @Pattern(regexp = "FRONTAL|POSTERIOR|LATERAL_IZQUIERDO|LATERAL_DERECHO|TECHO|LUNAS|LLANTAS|INTERIOR|OTRO",
                message = "La zona del dano no es valida") String zona,
        @NotBlank(message = "Describe el dano")
        @Size(max = 300, message = "La descripcion del dano no puede superar 300 caracteres") String descripcion,
        @NotNull(message = "La gravedad del dano es obligatoria")
        @Pattern(regexp = "LEVE|MODERADO|GRAVE", message = "La gravedad del dano no es valida") String gravedad,
        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 2 decimales") BigDecimal costoEstimado,
        @Pattern(regexp = "IZQUIERDA|DERECHA|FRENTE|ATRAS|SUPERIOR", message = "La vista del dano no es valida") String vista,
        @DecimalMin(value = "0", message = "La posicion del dano no es valida") @DecimalMax(value = "100", message = "La posicion del dano no es valida")
        @Digits(integer = 3, fraction = 2, message = "La posicion del dano no es valida") BigDecimal posX,
        @DecimalMin(value = "0", message = "La posicion del dano no es valida") @DecimalMax(value = "100", message = "La posicion del dano no es valida")
        @Digits(integer = 3, fraction = 2, message = "La posicion del dano no es valida") BigDecimal posY,
        @Pattern(regexp = "RAYON|ABOLLADURA|ROTURA|MANCHA|OTRO", message = "El tipo de dano no es valido") String tipo) {

    /** Dano sin ubicar en el dibujo. */
    public DanoDto(String zona, String descripcion, String gravedad, BigDecimal costoEstimado) {
        this(zona, descripcion, gravedad, costoEstimado, null, null, null, null);
    }
}
