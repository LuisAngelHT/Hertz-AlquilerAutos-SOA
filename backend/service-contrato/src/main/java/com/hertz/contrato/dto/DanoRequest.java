package com.hertz.contrato.dto;

import com.hertz.contrato.entity.GravedadDano;
import com.hertz.contrato.entity.TipoDano;
import com.hertz.contrato.entity.VistaVehiculo;
import com.hertz.contrato.entity.ZonaVehiculo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Un dano o defecto observado. El costo estimado (opcional) solo se usa en la devolucion. Si se marco en el
 * dibujo del vehiculo viene con su vista y su posicion (0 a 100); si no (por ejemplo un dano interior), sin ellas.
 */
public record DanoRequest(
        @NotNull(message = "La zona del dano es obligatoria") ZonaVehiculo zona,
        @NotBlank(message = "Describe el dano") @Size(max = 300, message = "La descripcion del dano no puede superar 300 caracteres") String descripcion,
        @NotNull(message = "La gravedad del dano es obligatoria") GravedadDano gravedad,
        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 2 decimales") BigDecimal costoEstimado,
        VistaVehiculo vista,
        @DecimalMin(value = "0", message = "La posicion del dano no es valida") @DecimalMax(value = "100", message = "La posicion del dano no es valida")
        @Digits(integer = 3, fraction = 2, message = "La posicion del dano no es valida") BigDecimal posX,
        @DecimalMin(value = "0", message = "La posicion del dano no es valida") @DecimalMax(value = "100", message = "La posicion del dano no es valida")
        @Digits(integer = 3, fraction = 2, message = "La posicion del dano no es valida") BigDecimal posY,
        TipoDano tipo) {

    /** Dano sin ubicar en el dibujo. */
    public DanoRequest(ZonaVehiculo zona, String descripcion, GravedadDano gravedad, BigDecimal costoEstimado) {
        this(zona, descripcion, gravedad, costoEstimado, null, null, null, null);
    }
}
