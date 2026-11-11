package com.hertz.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEn, String rol, String nombre) {
}