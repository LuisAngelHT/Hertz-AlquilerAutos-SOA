package com.hertz.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Lo que el propio cliente puede cambiar de su perfil: teléfono y licencia. El correo es su
 * identidad de acceso (lo usa el login) y el documento y los nombres los corrige el personal.
 */
public record ActualizarPerfilRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "El teléfono solo puede tener números, espacios, + , ( ) y -")
        String telefono,
        @NotBlank(message = "La licencia de conducir es obligatoria") @Size(max = 20) String licenciaConducir,
        @NotNull(message = "El vencimiento de la licencia es obligatorio") LocalDate vencimientoLicencia) {
}
