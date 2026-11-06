package com.hertz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo con formato inválido") String correo,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}