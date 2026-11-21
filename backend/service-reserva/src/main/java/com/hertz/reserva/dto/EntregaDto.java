package com.hertz.reserva.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Revision del vehiculo al ENTREGARLO: como sale, que danos ya tenia (para que el cliente no cargue con ellos) y que inventario lleva. */
public record EntregaDto(
        @NotNull(message = "El kilometraje es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = 2_000_000, message = "El kilometraje no es valido") Integer kilometraje,
        @NotNull(message = "El nivel de combustible es obligatorio")
        @Pattern(regexp = "VACIO|UN_CUARTO|MEDIO|TRES_CUARTOS|LLENO", message = "El nivel de combustible no es valido") String combustible,
        @Valid @Size(max = 30, message = "Se pueden registrar hasta 30 danos") List<DanoDto> danos,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres") String observaciones,
        @Valid @Size(max = 40, message = "El inventario no es valido") List<ItemDto> inventario) {

    /** Entrega sin inventario. */
    public EntregaDto(Integer kilometraje, String combustible, List<DanoDto> danos, String observaciones) {
        this(kilometraje, combustible, danos, observaciones, null);
    }
}
