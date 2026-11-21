package com.hertz.contrato.dto;

import com.hertz.contrato.entity.Combustible;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Revision del vehiculo al RECIBIRLO. Los danos son los NUEVOS (los que no tenia al salir): quedan a cargo del
 * cliente, igual que los elementos del inventario que faltan o vuelven danados. "clienteConforme" indica si el
 * cliente acepto lo registrado; si no, hay que explicar por que.
 */
public record DevolucionRequest(
        @NotNull(message = "El kilometraje es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Max(value = 2_000_000, message = "El kilometraje no es valido") Integer kilometraje,
        @NotNull(message = "El nivel de combustible es obligatorio") Combustible combustible,
        @Valid @Size(max = 30, message = "Se pueden registrar hasta 30 danos") List<DanoRequest> danos,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres") String observaciones,
        @NotNull(message = "Indica si el cliente esta conforme") Boolean clienteConforme,
        @Valid @Size(max = 40, message = "El inventario no es valido") List<ItemInventarioRequest> inventario) {

    /** Devolucion sin inventario. */
    public DevolucionRequest(Integer kilometraje, Combustible combustible, List<DanoRequest> danos,
                             String observaciones, Boolean clienteConforme) {
        this(kilometraje, combustible, danos, observaciones, clienteConforme, null);
    }

    public List<DanoRequest> danosONinguno() {
        return danos == null ? List.of() : danos;
    }

    public List<ItemInventarioRequest> inventarioONinguno() {
        return inventario == null ? List.of() : inventario;
    }
}
