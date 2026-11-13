package com.hertz.cliente.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ActualizarClienteRequest(
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email con formato inválido")
        @Size(max = 120) String email,
        @Size(max = 20) String telefono,
        @Size(max = 20) String licenciaConducir,
        LocalDate vencimientoLicencia) {
}