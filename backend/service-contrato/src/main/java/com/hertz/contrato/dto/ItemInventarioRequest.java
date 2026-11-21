package com.hertz.contrato.dto;

import com.hertz.contrato.entity.EstadoItem;
import com.hertz.contrato.entity.ItemInventario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Estado de un elemento del inventario. El costo (opcional) solo se toma en la devolucion. */
public record ItemInventarioRequest(
        @NotNull(message = "El elemento del inventario es obligatorio") ItemInventario item,
        @NotNull(message = "El estado del elemento es obligatorio") EstadoItem estado,
        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 2 decimales") BigDecimal costoEstimado) {
}
