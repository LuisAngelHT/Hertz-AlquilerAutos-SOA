package com.hertz.flota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de un vehículo nuevo en la flota (solo el ADMINISTRADOR general). Nace DISPONIBLE. */
public record CrearVehiculoRequest(
        @NotBlank(message = "La placa es obligatoria") @Size(max = 10, message = "La placa no puede pasar de 10 caracteres") String placa,
        @NotBlank(message = "La categoría es obligatoria") String categoria,
        @NotBlank(message = "La marca es obligatoria") @Size(max = 40, message = "La marca no puede pasar de 40 caracteres") String marca,
        @NotBlank(message = "El modelo es obligatorio") @Size(max = 40, message = "El modelo no puede pasar de 40 caracteres") String modelo,
        @NotNull(message = "El año es obligatorio") Integer anio,
        @NotBlank(message = "La sucursal es obligatoria") String sucursal) {
}
