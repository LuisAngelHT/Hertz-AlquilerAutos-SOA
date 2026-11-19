package com.hertz.incidencia.dto;

import com.hertz.incidencia.entity.TipoIncidencia;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record IncidenciaRequest(
        @NotBlank(message = "idVehiculo (la placa) es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9]{3}-[A-Za-z0-9]{3,4}", message = "La placa debe tener el formato ABC-123")
        String idVehiculo,
        UUID idContrato,
        @NotNull(message = "El tipo es obligatorio (DAÑO, MANTENIMIENTO o SINIESTRO)") TipoIncidencia tipo,
        @NotBlank(message = "La descripción es obligatoria") @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
        String descripcion,
        @DecimalMin(value = "0.0", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 8 enteros y 2 decimales")
        BigDecimal costoEstimado) {
}