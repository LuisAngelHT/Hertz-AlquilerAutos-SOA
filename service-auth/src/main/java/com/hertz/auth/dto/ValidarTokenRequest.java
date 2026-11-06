package com.hertz.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidarTokenRequest(@NotBlank(message = "El token es obligatorio") String token) {
}