package com.hertz.auth.dto;

import java.time.Instant;

// sucursal: sede del usuario (null si no tiene). El frontend la usa para saber qué vehículos puede modificar.
public record LoginResponse(String token, String tipo, Instant expiraEn, String rol, String nombre,
                            String sucursal) {
}