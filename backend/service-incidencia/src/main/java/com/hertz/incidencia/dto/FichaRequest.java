package com.hertz.incidencia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que el mecanico guarda en la ficha mientras la incidencia esta abierta. Las listas de danos se envian completas:
 * lo que no venga se quita (asi se pueden corregir o borrar danos mal anotados).
 */
public record FichaRequest(
        @Size(max = 1000, message = "El diagnostico no puede superar 1000 caracteres") String diagnostico,
        @Min(value = 0, message = "Los dias estimados no pueden ser negativos")
        @Max(value = 365, message = "Los dias estimados no pueden superar 365") Integer diasEstimados,
        @DecimalMin(value = "0.00", message = "El costo de repuestos no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo de repuestos admite hasta 2 decimales") BigDecimal costoRepuestos,
        @DecimalMin(value = "0.00", message = "El costo de mano de obra no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo de mano de obra admite hasta 2 decimales") BigDecimal costoManoObra,
        @Valid @Size(max = 30, message = "Se pueden registrar hasta 30 danos internos") List<DanoInternoRequest> internos,
        @Valid @Size(max = 30, message = "Se pueden registrar hasta 30 danos externos") List<DanoExternoRequest> externos) {

    public List<DanoInternoRequest> internosONinguno() {
        return internos == null ? List.of() : internos;
    }

    public List<DanoExternoRequest> externosONinguno() {
        return externos == null ? List.of() : externos;
    }
}
