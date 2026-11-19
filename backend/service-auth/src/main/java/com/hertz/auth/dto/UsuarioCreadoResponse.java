package com.hertz.auth.dto;

import java.util.UUID;

public record UsuarioCreadoResponse(UUID id, String correo, String rol) {
}