package com.hertz.flota.service;

import com.hertz.flota.dto.BloqueoRequest;
import com.hertz.flota.dto.BloqueoResponse;
import com.hertz.flota.dto.CrearVehiculoRequest;
import com.hertz.flota.dto.DisponibilidadResponse;
import com.hertz.flota.dto.HistorialResponse;
import com.hertz.flota.dto.PaginaVehiculosResponse;
import com.hertz.flota.dto.VehiculoResponse;
import com.hertz.flota.entity.BloqueoVehiculo;
import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.HistorialVehiculo;
import com.hertz.flota.entity.Vehiculo;
import com.hertz.flota.exception.FlotaException;
import com.hertz.flota.repository.BloqueoRepository;
import com.hertz.flota.repository.HistorialRepository;
import com.hertz.flota.repository.VehiculoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lógica de negocio de la flota. Aquí viven las reglas; el controlador solo recibe y responde.
 *
 * Reglas:
 *  - Un vehículo solo pasa a ALQUILADO si está DISPONIBLE.
 *  - Solo AGENTE, ADMINISTRADOR o ADMINISTRADOR_SUCURSAL pueden cambiar el estado.
 *  - Solo el ADMINISTRADOR general registra vehículos nuevos, los da de baja (FUERA_DE_SERVICIO) y los reactiva.
 *    La baja no se permite si el vehículo está alquilado o tiene reservas pendientes.
 *  - El ADMINISTRADOR general cambia vehículos de cualquier sucursal; el AGENTE y el
 *    ADMINISTRADOR_SUCURSAL solo los de su propia sucursal (header X-Sucursal).
 *  - La disponibilidad por fechas descarta los vehículos con un bloqueo que se cruce.
 *  - ServicioReserva aparta un vehiculo por fechas (bloqueo) de forma atomica y sin sobreventa.
 *  - Cada cambio de estado queda en el historial con quién lo hizo, cuándo y por qué.
 */
@Service
public class FlotaService {

    // Roles autorizados a cambiar el estado. El rol llega en el header X-Rol, que pone el gateway.
    // Tamaño de página del listado: por defecto 12 y nunca más de 50, aunque lo pidan.
    static final int TAMANO_POR_DEFECTO = 12;
    static final int TAMANO_MAXIMO = 50;

    // Valores válidos de un vehículo nuevo (los mismos que el check de la tabla y que usa reservas).
    static final Set<String> CATEGORIAS = Set.of("ECONOMICO", "SUV", "CAMIONETA", "PREMIUM");
    static final Set<String> SUCURSALES =
            Set.of("LIMA_AEROPUERTO", "LIMA_MIRAFLORES", "CUSCO_AEROPUERTO", "AREQUIPA_AEROPUERTO");
    // Placa: 3 letras o números, guion y 3 o 4 más (ABC-123, A1B-2345).
    private static final Pattern PLACA = Pattern.compile("^[A-Z0-9]{3}-[A-Z0-9]{3,4}$");
    private static final Set<String> MOTIVOS_PENDIENTES = Set.of("RESERVA", "CONTRATO");

    // Este rol puede modificar cualquier sucursal. Los demás roles de escritura, solo la suya.
    private static final String ROL_GENERAL = "ADMINISTRADOR";

    private static final Set<String> ROLES_ESCRITURA = Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL");

    private final VehiculoRepository vehiculos;
    private final HistorialRepository historial;
    private final BloqueoRepository bloqueos;

    public FlotaService(VehiculoRepository vehiculos, HistorialRepository historial, BloqueoRepository bloqueos) {
        this.vehiculos = vehiculos;
        this.historial = historial;
        this.bloqueos = bloqueos;
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
                                          String sucursalPrioritaria, Integer pagina, Integer tamano) {
        EstadoVehiculo estadoBuscado = estado == null ? EstadoVehiculo.DISPONIBLE : estado;
        String patronSucursal = vacio(sucursal) ? "%" : sucursal.trim().toUpperCase();
        String patronTexto = vacio(texto) ? "%" : "%" + texto.trim().toLowerCase() + "%";
        int paginaPedida = pagina == null ? 0 : Math.max(0, pagina);
        int tamanoPedido = tamano == null ? TAMANO_POR_DEFECTO : Math.min(TAMANO_MAXIMO, Math.max(1, tamano));

        String prioritaria = vacio(sucursalPrioritaria) ? "" : sucursalPrioritaria.trim().toUpperCase();
        Page<Vehiculo> resultado = vehiculos.listar(estadoBuscado, patronSucursal, patronTexto, prioritaria,
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
        String rolNormalizado = rol == null ? "" : rol.trim().toUpperCase();
        // SERVICIO es el rol interno con el que ServicioReserva marca el vehículo ALQUILADO en el
        // check-out y DISPONIBLE en la devolución (el documento: flota es el único que cambia el estado).
        boolean esServicio = ROL_SERVICIO.equals(rolNormalizado);
        if (!esServicio && !ROLES_ESCRITURA.contains(rolNormalizado)) {
            throw FlotaException.rolNoAutorizado();
        }
        Vehiculo vehiculo = buscar(placa);
        // Dar de baja, o reactivar un vehículo dado de baja, solo lo hace el ADMINISTRADOR general.
        boolean tocaBaja = nuevoEstado == EstadoVehiculo.FUERA_DE_SERVICIO
                || vehiculo.getEstado() == EstadoVehiculo.FUERA_DE_SERVICIO;
        if (tocaBaja && !ROL_GENERAL.equals(rolNormalizado)) {
            throw FlotaException.soloAdministradorGeneral();
        }
        // Fuera del ADMINISTRADOR general y del servicio interno, solo se modifican vehículos de la
        // propia sucursal. Sin sucursal asignada no se puede comprobar, así que también se rechaza.
        if (!ROL_GENERAL.equals(rolNormalizado) && !esServicio
                && (vacio(sucursalUsuario)
                    || !sucursalUsuario.trim().equalsIgnoreCase(vehiculo.getSucursal()))) {
            throw FlotaException.sucursalNoAutorizada(vehiculo.getPlaca(), vehiculo.getSucursal());
        }
        if (vehiculo.getEstado() == EstadoVehiculo.FUERA_DE_SERVICIO && nuevoEstado != EstadoVehiculo.DISPONIBLE) {
            throw FlotaException.transicionInvalida(
                    "Un vehiculo fuera de servicio solo puede reactivarse (pasar a DISPONIBLE)");
        }
        if (nuevoEstado == EstadoVehiculo.FUERA_DE_SERVICIO) {
            validarBaja(vehiculo);
        }
        if (nuevoEstado == EstadoVehiculo.ALQUILADO && vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw FlotaException.vehiculoNoDisponible(vehiculo.getPlaca());
        }
        EstadoVehiculo estadoAnterior = vehiculo.getEstado();
        vehiculo.cambiarEstado(nuevoEstado);
        Vehiculo guardado = vehiculos.save(vehiculo);
        historial.save(new HistorialVehiculo(guardado.getPlaca(), estadoAnterior, nuevoEstado,
                vacio(usuario) ? "DESCONOCIDO" : usuario.trim(), rolNormalizado,
                vacio(motivo) ? null : motivo.trim()));
        return VehiculoResponse.de(guardado);
    }

    // Rol interno con el que ServicioReserva llama a flota (no existe como rol de usuario).
    static final String ROL_SERVICIO = "SERVICIO";
    static final String MOTIVO_RESERVA = "RESERVA";

    /**
     * Aparta un vehiculo para una reserva: elige uno DISPONIBLE de la sucursal y categoria que
     * este libre en todo el rango, y crea el bloqueo con la referencia (id de la reserva).
     * Es atomico y concurrente: la fila del vehiculo se bloquea (FOR UPDATE SKIP LOCKED), asi que
     * dos reservas simultaneas nunca reciben el mismo vehiculo. Es idempotente: repetir la misma
     * referencia devuelve el bloqueo que ya existe (reintentos seguros ante cortes de red).
     * Sin vehiculos libres -> 409 SIN_DISPONIBILIDAD.
     */
    @Transactional
    public BloqueoResponse apartar(BloqueoRequest pedido, String rol) {
        exigirServicio(rol);
        validarFechas(pedido);
        Optional<BloqueoVehiculo> existente = bloqueos.findFirstByReferenciaAndMotivo(
                pedido.referencia().trim(), MOTIVO_RESERVA);
        if (existente.isPresent()) {
            return aBloqueoResponse(existente.get());
        }
        return crearBloqueo(pedido, null);
    }

    /**
     * Cambia las fechas o la categoria de una reserva ya apartada, en una sola transaccion: libera
     * el bloqueo anterior y aparta uno nuevo. Si no hay disponibilidad se deshace todo y la
     * reserva conserva su vehiculo original.
     */
    @Transactional
    public BloqueoResponse reprogramar(String referencia, BloqueoRequest pedido, String rol) {
        exigirServicio(rol);
        validarFechas(pedido);
        BloqueoVehiculo actual = bloqueos.findFirstByReferenciaAndMotivo(referencia.trim(), MOTIVO_RESERVA)
                .orElseThrow(() -> FlotaException.bloqueoNoEncontrado(referencia));
        bloqueos.delete(actual);
        bloqueos.flush();
        // Al cambiar fechas se intenta conservar el MISMO vehiculo; si ya no esta libre, se busca otro de la categoria.
        return crearBloqueo(new BloqueoRequest(pedido.sucursal(), pedido.categoria(),
                pedido.fechaInicio(), pedido.fechaFin(), referencia.trim(), null), actual.getPlaca());
    }

    /** Libera el vehiculo apartado por una referencia. Es idempotente: si ya no existe, no falla. */
    @Transactional
    public void liberar(String referencia, String rol) {
        exigirServicio(rol);
        bloqueos.findFirstByReferenciaAndMotivo(referencia.trim(), MOTIVO_RESERVA).ifPresent(bloqueos::delete);
    }

    @Transactional(readOnly = true)
    public BloqueoResponse consultarBloqueo(String referencia, String rol) {
        exigirServicio(rol);
        return bloqueos.findFirstByReferenciaAndMotivo(referencia.trim(), MOTIVO_RESERVA)
                .map(this::aBloqueoResponse)
                .orElseThrow(() -> FlotaException.bloqueoNoEncontrado(referencia));
    }

    // placaPreferida: al reprogramar, el vehiculo que ya tenia la reserva (se intenta conservar).
    private BloqueoResponse crearBloqueo(BloqueoRequest pedido, String placaPreferida) {
        String sucursal = pedido.sucursal().trim().toUpperCase();
        String categoria = pedido.categoria().trim().toUpperCase();
        List<Vehiculo> libres = List.of();
        if (!vacio(pedido.placa())) {
            // El cliente eligio un vehiculo concreto: se aparta ese o ninguno.
            String elegida = pedido.placa().trim().toUpperCase();
            libres = vehiculos.tomarDisponiblePorPlaca(elegida, sucursal, categoria, EstadoVehiculo.DISPONIBLE,
                    pedido.fechaInicio(), pedido.fechaFin());
            if (libres.isEmpty()) {
                throw FlotaException.vehiculoElegidoNoDisponible(elegida);
            }
        } else {
            if (placaPreferida != null) {
                libres = vehiculos.tomarDisponiblePorPlaca(placaPreferida, sucursal, categoria, EstadoVehiculo.DISPONIBLE,
                        pedido.fechaInicio(), pedido.fechaFin());
            }
            if (libres.isEmpty()) {
                libres = vehiculos.tomarDisponibles(sucursal, categoria, EstadoVehiculo.DISPONIBLE,
                        pedido.fechaInicio(), pedido.fechaFin(), PageRequest.of(0, 1));
            }
            if (libres.isEmpty()) {
                throw FlotaException.sinDisponibilidad(categoria, sucursal);
            }
        }
        Vehiculo elegido = libres.get(0);
        BloqueoVehiculo bloqueo = bloqueos.save(new BloqueoVehiculo(elegido.getPlaca(),
                pedido.fechaInicio(), pedido.fechaFin(), MOTIVO_RESERVA, pedido.referencia().trim()));
        return BloqueoResponse.de(bloqueo, elegido.getSucursal(), elegido.getCategoria());
    }

    private BloqueoResponse aBloqueoResponse(BloqueoVehiculo b) {
        Vehiculo v = buscar(b.getPlaca());
        return BloqueoResponse.de(b, v.getSucursal(), v.getCategoria());
    }

    private static void exigirServicio(String rol) {
        if (rol == null || !ROL_SERVICIO.equals(rol.trim().toUpperCase())) {
            throw FlotaException.soloServicios();
        }
    }

    private static void validarFechas(BloqueoRequest pedido) {
        if (pedido.fechaFin().isBefore(pedido.fechaInicio())) {
            throw FlotaException.solicitudInvalida("fechaFin no puede ser anterior a fechaInicio");
        }
    }

    /**
     * Registra un vehículo nuevo (la empresa adquirió una unidad). Solo el ADMINISTRADOR general.
     * Nace DISPONIBLE en la sucursal indicada, y el alta queda en el historial con quién la hizo.
     * Valida el formato de la placa, que no exista, la categoría, la sucursal y el año.
     */
    @Transactional
    public VehiculoResponse crearVehiculo(CrearVehiculoRequest pedido, String rol, String usuario) {
        if (rol == null || !ROL_GENERAL.equals(rol.trim().toUpperCase())) {
            throw FlotaException.soloAdministradorGeneral();
        }
        String placa = pedido.placa().trim().toUpperCase();
        if (!PLACA.matcher(placa).matches()) {
            throw FlotaException.solicitudInvalida("La placa debe tener el formato ABC-123 (3 letras o numeros, guion, 3 o 4 mas)");
        }
        String categoria = pedido.categoria().trim().toUpperCase();
        if (!CATEGORIAS.contains(categoria)) {
            throw FlotaException.solicitudInvalida("Categoria invalida; usa " + String.join(", ", CATEGORIAS));
        }
        String sucursal = pedido.sucursal().trim().toUpperCase();
        if (!SUCURSALES.contains(sucursal)) {
            throw FlotaException.solicitudInvalida("Sucursal invalida; usa " + String.join(", ", SUCURSALES));
        }
        int maxAnio = Year.now(ZoneId.of("America/Lima")).getValue() + 1;
        if (pedido.anio() < 2000 || pedido.anio() > maxAnio) {
            throw FlotaException.solicitudInvalida("El anio debe estar entre 2000 y " + maxAnio);
        }
        if (vehiculos.existsById(placa)) {
            throw FlotaException.placaDuplicada(placa);
        }
        Vehiculo nuevo = vehiculos.save(new Vehiculo(placa, categoria, pedido.marca().trim(), pedido.modelo().trim(),
                pedido.anio().shortValue(), sucursal));
        historial.save(new HistorialVehiculo(nuevo.getPlaca(), null, EstadoVehiculo.DISPONIBLE,
                vacio(usuario) ? "DESCONOCIDO" : usuario.trim(), ROL_GENERAL, "Alta del vehiculo en la flota"));
        return VehiculoResponse.de(nuevo);
    }

    // No se da de baja un vehículo con un cliente, ni uno con reservas o contratos pendientes.
    private void validarBaja(Vehiculo vehiculo) {
        if (vehiculo.getEstado() == EstadoVehiculo.ALQUILADO) {
            throw FlotaException.vehiculoAlquilado(vehiculo.getPlaca());
        }
        long pendientes = bloqueos.countByPlacaAndFechaFinGreaterThanEqualAndMotivoIn(
                vehiculo.getPlaca(), LocalDate.now(ZoneId.of("America/Lima")), MOTIVOS_PENDIENTES);
        if (pendientes > 0) {
            throw FlotaException.vehiculoConReservas(vehiculo.getPlaca(), pendientes);
        }
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
