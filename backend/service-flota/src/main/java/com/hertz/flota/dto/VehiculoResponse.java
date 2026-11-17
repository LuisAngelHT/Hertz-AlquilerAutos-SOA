package com.hertz.flota.dto;

import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;

/**
 * Datos de un vehículo que devuelve la API. Se usa un DTO y no la entidad para no exponer
 * detalles internos de la base. "idVehiculo" y "placa" valen lo mismo: el contrato del
 * documento llama idVehiculo a la placa.
 */
public record VehiculoResponse(String idVehiculo, String placa, String categoria, String marca,
                               String modelo, short anio, EstadoVehiculo estado, String sucursal) {

    public static VehiculoResponse de(Vehiculo v) {
        return new VehiculoResponse(v.getPlaca(), v.getPlaca(), v.getCategoria(), v.getMarca(),
                v.getModelo(), v.getAnio(), v.getEstado(), v.getSucursal());
    }
}
