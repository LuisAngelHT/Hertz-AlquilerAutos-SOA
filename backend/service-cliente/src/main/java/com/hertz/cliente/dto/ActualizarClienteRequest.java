package com.hertz.cliente.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ActualizarClienteRequest(
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email con formato inválido")
        @Size(max = 120) String email,
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "El teléfono solo puede tener números, espacios, + , ( ) y -")
        String telefono,
        @NotBlank(message = "La licencia de conducir es obligatoria") @Size(max = 20) String licenciaConducir,
        @NotNull(message = "El vencimiento de la licencia es obligatorio") LocalDate vencimientoLicencia) {
}