package com.hertz.contrato.dto;

import com.hertz.contrato.entity.Combustible;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Revision del vehiculo al ENTREGARLO: como sale, que danos ya tiene y que inventario lleva. */
public record InspeccionRequest(
        @NotNull(message = "El kilometraje es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = 2_000_000, message = "El kilometraje no es valido") Integer kilometraje,
        @NotNull(message = "El nivel de combustible es obligatorio") Combustible combustible,
        @Valid @Size(max = 30, message = "Se pueden registrar hasta 30 danos") List<DanoRequest> danos,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres") String observaciones,
        @Valid @Size(max = 40, message = "El inventario no es valido") List<ItemInventarioRequest> inventario) {

    /** Entrega sin inventario. */
    public InspeccionRequest(Integer kilometraje, Combustible combustible, List<DanoRequest> danos, String observaciones) {
        this(kilometraje, combustible, danos, observaciones, null);
    }

    public List<DanoRequest> danosONinguno() {
        return danos == null ? List.of() : danos;
    }

    public List<ItemInventarioRequest> inventarioONinguno() {
        return inventario == null ? List.of() : inventario;
    }
}
