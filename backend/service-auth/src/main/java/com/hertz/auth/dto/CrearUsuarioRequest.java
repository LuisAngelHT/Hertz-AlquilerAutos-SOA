package com.hertz.auth.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CrearUsuarioRequest(
        @NotBlank @Email String correo,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UUID clienteId) {

    @Override
    public String toString() {
        return "CrearUsuarioRequest[correo=" + correo + "]";
    }
}