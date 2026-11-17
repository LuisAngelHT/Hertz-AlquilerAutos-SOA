package com.hertz.flota.service;

import com.hertz.flota.dto.DisponibilidadResponse;
import com.hertz.flota.dto.VehiculoResponse;
import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;
import com.hertz.flota.exception.FlotaException;
import com.hertz.flota.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Lógica de negocio de la flota. Aquí viven las reglas; el controlador solo recibe y responde.
 *
 * Reglas:
 *  - Un vehículo solo pasa a ALQUILADO si está DISPONIBLE.
 *  - Solo AGENTE o ADMINISTRADOR pueden cambiar el estado.
 *  - La disponibilidad por fechas descarta los vehículos con un bloqueo que se cruce.
 */
@Service
public class FlotaService {

    // Roles autorizados a cambiar el estado. El rol llega en el header X-Rol, que pone el gateway.
    private static final Set<String> ROLES_ESCRITURA = Set.of("AGENTE", "ADMINISTRADOR");

    private final VehiculoRepository vehiculos;

    public FlotaService(VehiculoRepository vehiculos) {
        this.vehiculos = vehiculos;
    }

    /**
     * Devuelve los vehículos DISPONIBLES de una sucursal y categoría que estén libres en todo
     * el rango [fechaInicio, fechaFin]. Normaliza el texto (sin espacios, en mayúsculas) para
     * que "suv" y "SUV" den lo mismo.
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(String sucursal, String categoria,
                                                          LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw FlotaException.solicitudInvalida("fechaFin no puede ser anterior a fechaInicio");
        }
        String categoriaNormalizada = categoria.trim().toUpperCase();
        List<VehiculoResponse> lista = vehiculos
                .buscarDisponibles(sucursal.trim().toUpperCase(), categoriaNormalizada,
                        EstadoVehiculo.DISPONIBLE, fechaInicio, fechaFin)
                .stream().map(VehiculoResponse::de).toList();
        return new DisponibilidadResponse(categoriaNormalizada, lista.size(), lista);
    }

    /** Busca un vehículo por placa. Lanza VEHICULO_NO_ENCONTRADO (404) si no existe. */
    @Transactional(readOnly = true)
    public VehiculoResponse consultarVehiculo(String placa) {
        return VehiculoResponse.de(buscar(placa));
    }

    /**
     * Cambia el estado de un vehículo. Orden de las validaciones:
     * 1) el rol puede escribir (si no, 403), 2) el vehículo existe (si no, 404),
     * 3) para ALQUILADO debe estar DISPONIBLE (si no, 409). Volver a DISPONIBLE o pasar a
     * MANTENIMIENTO se permite desde cualquier estado.
     */
    @Transactional
    public VehiculoResponse actualizarEstado(String placa, EstadoVehiculo nuevoEstado, String rol) {
        if (rol == null || !ROLES_ESCRITURA.contains(rol.trim().toUpperCase())) {
            throw FlotaException.rolNoAutorizado();
        }
        Vehiculo vehiculo = buscar(placa);
        if (nuevoEstado == EstadoVehiculo.ALQUILADO && vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw FlotaException.vehiculoNoDisponible(vehiculo.getPlaca());
        }
        vehiculo.cambiarEstado(nuevoEstado);
        return VehiculoResponse.de(vehiculos.save(vehiculo));
    }

    // Busca por placa ignorando mayúsculas y espacios; si no existe lanza 404.
    private Vehiculo buscar(String placa) {
        String clave = placa.trim().toUpperCase();
        return vehiculos.findById(clave).orElseThrow(() -> FlotaException.vehiculoNoEncontrado(clave));
    }
}
