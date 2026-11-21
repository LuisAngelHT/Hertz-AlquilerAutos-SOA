package com.hertz.reserva.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Estado de un elemento del inventario en la revision (se reenvia al servicio de contratos). "nuevo" lo marca la
 * pantalla cuando el elemento esta peor que al entregar: solo sirve para armar la incidencia y no se reenvia.
 */
public record ItemDto(
        @NotNull(message = "El elemento del inventario es obligatorio")
        @Pattern(regexp = "LUCES_PRINCIPALES|LUCES_FRENO_DIRECCIONALES|LIMPIAPARABRISAS|ESPEJOS_LATERALES|PARABRISAS_Y_LUNAS|LLANTAS_Y_TAPONES|TAPON_GASOLINA|PLACAS|RADIO|ESPEJO_RETROVISOR|CINTURONES|TAPETES|ASIENTOS_Y_FUNDAS|MANIJAS_Y_SEGUROS|AIRE_ACONDICIONADO|GATA|LLAVE_DE_RUEDAS|LLANTA_DE_AUXILIO|TRIANGULO|EXTINTOR|BOTIQUIN|SOAT|REVISION_TECNICA|TARJETA_DE_PROPIEDAD", message = "El elemento del inventario no es valido") String item,
        @NotNull(message = "El estado del elemento es obligatorio")
        @Pattern(regexp = "OK|FALTA|DANADO", message = "El estado del elemento no es valido") String estado,
        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo estimado admite hasta 2 decimales") BigDecimal costoEstimado,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) Boolean nuevo) {

    public ItemDto(String item, String estado, BigDecimal costoEstimado) {
        this(item, estado, costoEstimado, null);
    }
}
