package com.hertz.auth.dto;

import java.time.Instant;

public record ValidarTokenResponse(boolean valido, String correo, String rol, Instant expiraEn) {
}