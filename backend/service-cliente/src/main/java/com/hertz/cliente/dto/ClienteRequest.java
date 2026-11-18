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
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "El teléfono solo puede tener números, espacios, + , ( ) y -")
        String telefono,
        @NotBlank(message = "La licencia de conducir es obligatoria") @Size(max = 20) String licenciaConducir,
        @NotNull(message = "El vencimiento de la licencia es obligatorio") LocalDate vencimientoLicencia,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {

    // Evita que la contraseña aparezca si alguien imprime el objeto en un log
    @Override
    public String toString() {
        return "ClienteRequest[tipoDocumento=" + tipoDocumento + ", numeroDocumento=" + numeroDocumento + "]";
    }
}