package com.hertz.flota.service;

import com.hertz.flota.dto.DisponibilidadResponse;
import com.hertz.flota.dto.HistorialResponse;
import com.hertz.flota.dto.PaginaVehiculosResponse;
import com.hertz.flota.dto.VehiculoResponse;
import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.HistorialVehiculo;
import com.hertz.flota.entity.Vehiculo;
import com.hertz.flota.exception.FlotaException;
import com.hertz.flota.repository.HistorialRepository;
import com.hertz.flota.repository.VehiculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
 *  - Solo AGENTE, ADMINISTRADOR o ADMINISTRADOR_SUCURSAL pueden cambiar el estado.
 *  - El ADMINISTRADOR general cambia vehículos de cualquier sucursal; el AGENTE y el
 *    ADMINISTRADOR_SUCURSAL solo los de su propia sucursal (header X-Sucursal).
 *  - La disponibilidad por fechas descarta los vehículos con un bloqueo que se cruce.
 *  - Cada cambio de estado queda en el historial con quién lo hizo, cuándo y por qué.
 */
@Service
public class FlotaService {

    // Roles autorizados a cambiar el estado. El rol llega en el header X-Rol, que pone el gateway.
    // Tamaño de página del listado: por defecto 12 y nunca más de 50, aunque lo pidan.
    static final int TAMANO_POR_DEFECTO = 12;
    static final int TAMANO_MAXIMO = 50;

    // Este rol puede modificar cualquier sucursal. Los demás roles de escritura, solo la suya.
    private static final String ROL_GENERAL = "ADMINISTRADOR";

    private static final Set<String> ROLES_ESCRITURA = Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL");

    private final VehiculoRepository vehiculos;
    private final HistorialRepository historial;

    public FlotaService(VehiculoRepository vehiculos, HistorialRepository historial) {
        this.vehiculos = vehiculos;
        this.historial = historial;
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
     * Lista los vehículos en un estado (por defecto DISPONIBLE), con filtros opcionales por
     * sucursal y por texto (marca o modelo, sin importar mayúsculas). Sin filtros devuelve todos.
     * Sirve para la vista del cliente (buscar "toyota" o "rav4") y para la vista del personal.
     * Devuelve una página (pagina empieza en 0; tamano entre 1 y 50, por defecto 12) junto con el
     * total, para que la pantalla no tenga que mostrar cientos de vehículos juntos.
     */
    @Transactional(readOnly = true)
    public PaginaVehiculosResponse listar(EstadoVehiculo estado, String sucursal, String texto,
                                          Integer pagina, Integer tamano) {
        EstadoVehiculo estadoBuscado = estado == null ? EstadoVehiculo.DISPONIBLE : estado;
        String patronSucursal = vacio(sucursal) ? "%" : sucursal.trim().toUpperCase();
        String patronTexto = vacio(texto) ? "%" : "%" + texto.trim().toLowerCase() + "%";
        int paginaPedida = pagina == null ? 0 : Math.max(0, pagina);
        int tamanoPedido = tamano == null ? TAMANO_POR_DEFECTO : Math.min(TAMANO_MAXIMO, Math.max(1, tamano));

        Page<Vehiculo> resultado = vehiculos.listar(estadoBuscado, patronSucursal, patronTexto,
                PageRequest.of(paginaPedida, tamanoPedido));
        return new PaginaVehiculosResponse(resultado.getTotalElements(), paginaPedida, tamanoPedido,
                resultado.getTotalPages(), resultado.getContent().stream().map(VehiculoResponse::de).toList());
    }

    /** Historial de cambios de estado de un vehículo, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<HistorialResponse> consultarHistorial(String placa) {
        Vehiculo vehiculo = buscar(placa);
        return historial.findByPlacaOrderByFechaDesc(vehiculo.getPlaca())
                .stream().map(HistorialResponse::de).toList();
    }

    /**
     * Cambia el estado de un vehículo. Orden de las validaciones:
     * 1) el rol puede escribir (si no, 403), 2) el vehículo existe (si no, 404),
     * 3) para ALQUILADO debe estar DISPONIBLE (si no, 409). Volver a DISPONIBLE o pasar a
     * MANTENIMIENTO se permite desde cualquier estado.
     * El cambio y su registro en el historial se guardan en la misma transacción: o se
     * guardan los dos o ninguno.
     */
    @Transactional
    public VehiculoResponse actualizarEstado(String placa, EstadoVehiculo nuevoEstado, String rol,
                                             String usuario, String sucursalUsuario, String motivo) {
        if (rol == null || !ROLES_ESCRITURA.contains(rol.trim().toUpperCase())) {
            throw FlotaException.rolNoAutorizado();
        }
        Vehiculo vehiculo = buscar(placa);
        // Fuera del ADMINISTRADOR general, solo se modifican vehículos de la propia sucursal.
        // Sin sucursal asignada no se puede comprobar, así que también se rechaza.
        if (!ROL_GENERAL.equals(rol.trim().toUpperCase())
                && (vacio(sucursalUsuario)
                    || !sucursalUsuario.trim().equalsIgnoreCase(vehiculo.getSucursal()))) {
            throw FlotaException.sucursalNoAutorizada(vehiculo.getPlaca(), vehiculo.getSucursal());
        }
        if (nuevoEstado == EstadoVehiculo.ALQUILADO && vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw FlotaException.vehiculoNoDisponible(vehiculo.getPlaca());
        }
        EstadoVehiculo estadoAnterior = vehiculo.getEstado();
        vehiculo.cambiarEstado(nuevoEstado);
        Vehiculo guardado = vehiculos.save(vehiculo);
        historial.save(new HistorialVehiculo(guardado.getPlaca(), estadoAnterior, nuevoEstado,
                vacio(usuario) ? "DESCONOCIDO" : usuario.trim(), rol.trim().toUpperCase(),
                vacio(motivo) ? null : motivo.trim()));
        return VehiculoResponse.de(guardado);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    // Busca por placa ignorando mayúsculas y espacios; si no existe lanza 404.
    private Vehiculo buscar(String placa) {
        String clave = placa.trim().toUpperCase();
        return vehiculos.findById(clave).orElseThrow(() -> FlotaException.vehiculoNoEncontrado(clave));
    }
}
