package com.hertz.cliente.dto;

import com.hertz.cliente.entity.TipoDocumento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClienteResponse(
        UUID idCliente,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String email,
        String telefono,
        String licenciaConducir,
        LocalDate vencimientoLicencia,
        boolean licenciaVigente,
        List<Object> historialAlquileres) {
}