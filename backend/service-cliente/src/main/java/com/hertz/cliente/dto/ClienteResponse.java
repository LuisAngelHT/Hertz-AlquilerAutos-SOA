package com.hertz.cliente.dto;

import com.hertz.cliente.entity.TipoDocumento;

import java.util.List;
import java.util.UUID;

public record ClienteResponse(
        UUID idCliente,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        boolean licenciaVigente,
        List<Object> historialAlquileres) {
}