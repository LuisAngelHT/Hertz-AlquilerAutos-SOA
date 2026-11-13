package com.hertz.cliente.dto;

import com.hertz.cliente.entity.TipoDocumento;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClienteRequest(
        @NotNull(message = "El tipo de documento es obligatorio") TipoDocumento tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio") @Size(max = 20) String numeroDocumento,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email con formato inválido")
        @Size(max = 120) String email,
        @Size(max = 20) String telefono,
        @Size(max = 20) String licenciaConducir,
        LocalDate vencimientoLicencia) {
}