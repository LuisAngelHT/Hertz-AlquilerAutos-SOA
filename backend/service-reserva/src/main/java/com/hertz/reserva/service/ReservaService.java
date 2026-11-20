package com.hertz.reserva.service;

import com.hertz.reserva.client.ClienteClient;
import com.hertz.reserva.client.ClienteClient.ClienteInfo;
import com.hertz.reserva.client.ContratoClient;
import com.hertz.reserva.client.FlotaClient;
import com.hertz.reserva.dto.HistorialReservaResponse;
import com.hertz.reserva.dto.ModificarReservaRequest;
import com.hertz.reserva.dto.PaginaReservasResponse;
import com.hertz.reserva.dto.ReservaRequest;
import com.hertz.reserva.dto.ReservaResponse;
import com.hertz.reserva.entity.EstadoReserva;
import com.hertz.reserva.entity.HistorialReserva;
import com.hertz.reserva.entity.Reserva;
import com.hertz.reserva.exception.ReservaException;
import com.hertz.reserva.repository.HistorialReservaRepository;
import com.hertz.reserva.repository.ReservaRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lógica de negocio de las reservas (SVC-02). El controlador solo recibe y responde.
 *
 * Reglas principales:
 *  - Al crear: el cliente debe existir (ServicioCliente) y debe haber un vehículo de la categoría
 *    libre en esas fechas; ServicioFlota lo aparta de forma atómica. La reserva nace CONFIRMADA.
 *  - Estados: CONFIRMADA -> EN_CURSO | CANCELADA; EN_CURSO -> FINALIZADA.
 *  - Roles: AGENTE, ADMINISTRADOR y ADMINISTRADOR_SUCURSAL gestionan reservas; el CLIENTE solo
 *    crea, consulta, modifica y cancela las SUYAS; el rol interno SERVICIO (el orquestador) cambia estados.
 *  - Sucursales: el personal de sucursal (todo menos el ADMINISTRADOR general) solo VE las reservas con
 *    recojo o devolución en su sede, solo CREA reservas con recojo en su sede y solo las gestiona si el
 *    recojo es suyo (la devolución del vehículo también la registra la sede de devolución).
 *  - Cada cliente puede tener como máximo 2 reservas activas (CONFIRMADA o EN_CURSO) y necesita una
 *    licencia vigente hasta el último día del alquiler.
 *  - Cada cambio queda en el historial con quién lo hizo.
 *
 * Consistencia entre servicios (sin transacciones distribuidas): primero se aparta el vehículo
 * en flota, luego se guarda la reserva; si el guardado falla se libera el vehículo (compensación).
 * Las llamadas a flota son idempotentes, así que cualquier paso se puede reintentar.
 * Nunca se mantiene una conexión de base de datos abierta mientras se espera a otro servicio.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    public static final Set<String> CATEGORIAS = Set.of("ECONOMICO", "SUV", "CAMIONETA", "PREMIUM");
    public static final Set<String> SUCURSALES =
            Set.of("LIMA_AEROPUERTO", "LIMA_MIRAFLORES", "CUSCO_AEROPUERTO", "AREQUIPA_AEROPUERTO");

    static final int TAMANO_POR_DEFECTO = 12;
    static final int TAMANO_MAXIMO = 50;
    static final int MAX_RESERVAS_ACTIVAS = 2;

    private static final String ROL_GENERAL = "ADMINISTRADOR";
    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String ROL_SERVICIO = "SERVICIO";
    private static final Set<String> ROLES_PERSONAL = Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL");

    /** Quién hace la solicitud: lo que el gateway puso en X-Usuario, X-Rol y X-Sucursal. */
    public record Actor(String usuario, String rol, String sucursal) {
        String rolNormalizado() {
            return rol == null ? "" : rol.trim().toUpperCase();
        }

        String usuarioONombre() {
            return usuario == null || usuario.isBlank() ? "DESCONOCIDO" : usuario.trim();
        }
    }

    private final ReservaRepository reservas;
    private final HistorialReservaRepository historial;
    private final FlotaClient flota;
    private final ClienteClient clientes;
    private final ContratoClient contratos;
    private final TransactionTemplate tx;
    private final Clock reloj;
    private final int maxDias;

    public ReservaService(ReservaRepository reservas, HistorialReservaRepository historial,
                          FlotaClient flota, ClienteClient clientes, ContratoClient contratos,
                          PlatformTransactionManager transacciones, Clock reloj,
                          @Value("${reserva.max-dias:60}") int maxDias) {
        this.reservas = reservas;
        this.historial = historial;
        this.flota = flota;
        this.clientes = clientes;
        this.contratos = contratos;
        this.tx = new TransactionTemplate(transacciones);
        this.reloj = reloj;
        this.maxDias = maxDias;
    }

    // ------------------------------------------------------------------ crear

    public ReservaResponse crear(ReservaRequest pedido, Actor actor) {
        String rol = actor.rolNormalizado();
        if (!ROLES_PERSONAL.contains(rol) && !ROL_CLIENTE.equals(rol)) {
            throw ReservaException.rolNoAutorizado("Tu rol no puede crear reservas");
        }
        String categoria = categoriaValida(pedido.categoria());
        String recojo = sucursalValida(pedido.sucursalRecojo(), "sucursalRecojo");
        String devolucion = sucursalValida(pedido.sucursalDevolucion(), "sucursalDevolucion");
        validarFechas(pedido.fechaInicio(), pedido.fechaFin(), true);
        // El personal de sucursal solo crea reservas que se recogen en su propia sede.
        if (esPersonalDeSucursal(rol) && !esDeSuSede(actor, recojo)) {
            throw ReservaException.sucursalNoAutorizada(recojo);
        }
        ClienteInfo cliente = resolverCliente(pedido.idCliente(), actor, rol);
        exigirLicenciaVigente(cliente, pedido.fechaFin());
        if (reservas.countByIdClienteAndEstadoIn(cliente.idCliente(),
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO)) >= MAX_RESERVAS_ACTIVAS) {
            throw ReservaException.limiteReservas(MAX_RESERVAS_ACTIVAS);
        }

        UUID id = UUID.randomUUID();
        var apartado = flota.apartar(id.toString(), recojo, categoria, pedido.fechaInicio(), pedido.fechaFin(),
                esVacio(pedido.placa()) ? null : pedido.placa().trim().toUpperCase());
        try {
            return tx.execute(estado -> {
                Reserva guardada = reservas.save(new Reserva(id, cliente.idCliente(), categoria, recojo, devolucion,
                        pedido.fechaInicio(), pedido.fechaFin(), apartado.placa(), actor.usuarioONombre(),
                        cliente.nombreCompleto(), cliente.documento(), cliente.telefono()));
                historial.save(new HistorialReserva(id, "CREADA", null, EstadoReserva.CONFIRMADA,
                        actor.usuarioONombre(), rol,
                        categoria + " del " + pedido.fechaInicio() + " al " + pedido.fechaFin() + ", recojo en " + recojo));
                return ReservaResponse.de(guardada);
            });
        } catch (RuntimeException ex) {
            liberarSinFallar(id);   // compensación: no dejar un vehículo apartado sin reserva
            throw ex;
        }
    }

    // ---------------------------------------------------------------- consultar

    public ReservaResponse obtener(UUID id, Actor actor) {
        Reserva reserva = cargar(id);
        autorizarLectura(reserva, actor);
        return ReservaResponse.de(reserva);
    }

    /**
     * Lista reservas con filtros opcionales y paginación. El CLIENTE solo ve las suyas (se ignoran
     * los demás filtros de cliente). El personal puede filtrar por idCliente o por documento.
     */
    public PaginaReservasResponse listar(String tipoDocumento, String numeroDocumento, UUID idCliente,
                                         EstadoReserva estado, String sucursal, Boolean recogenHoy,
                                         Integer pagina, Integer tamano, Actor actor) {
        String rol = actor.rolNormalizado();
        if (!ROLES_PERSONAL.contains(rol) && !ROL_CLIENTE.equals(rol) && !ROL_SERVICIO.equals(rol)) {
            throw ReservaException.rolNoAutorizado("Tu rol no puede consultar reservas");
        }
        UUID filtroCliente = idCliente;
        if (ROL_CLIENTE.equals(rol)) {
            filtroCliente = clientes.porCorreo(actor.usuario()).idCliente();
        } else if (!esVacio(tipoDocumento) || !esVacio(numeroDocumento)) {
            if (esVacio(tipoDocumento) || esVacio(numeroDocumento)) {
                throw ReservaException.solicitudInvalida("Para buscar por documento envia tipoDocumento y numeroDocumento");
            }
            var cliente = clientes.porDocumento(tipoDocumento.trim().toUpperCase(), numeroDocumento.trim());
            if (cliente.isEmpty()) {
                int p = Math.max(0, pagina == null ? 0 : pagina);
                return new PaginaReservasResponse(0, p, tamanoValido(tamano), 0, List.of());
            }
            filtroCliente = cliente.get().idCliente();
        }

        final UUID clienteFinal = filtroCliente;
        final String sucursalFinal = esVacio(sucursal) ? null : sucursal.trim().toUpperCase();
        // El personal de sucursal solo ve reservas que salen o llegan a su sede (sin sede asignada, ninguna).
        final String sedeDelPersonal = esPersonalDeSucursal(rol)
                ? (esVacio(actor.sucursal()) ? "-" : actor.sucursal().trim().toUpperCase()) : null;
        final LocalDate hoy = LocalDate.now(reloj);
        final boolean soloHoy = Boolean.TRUE.equals(recogenHoy);
        Specification<Reserva> filtros = (root, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (sedeDelPersonal != null) {
                condiciones.add(cb.or(cb.equal(root.get("sucursalRecojo"), sedeDelPersonal),
                        cb.equal(root.get("sucursalDevolucion"), sedeDelPersonal)));
            }
            if (soloHoy) {   // "Recogen hoy": reservas confirmadas que empiezan hoy
                condiciones.add(cb.equal(root.get("fechaInicio"), hoy));
                condiciones.add(cb.equal(root.get("estado"), EstadoReserva.CONFIRMADA));
            }
            if (clienteFinal != null) condiciones.add(cb.equal(root.get("idCliente"), clienteFinal));
            if (estado != null) condiciones.add(cb.equal(root.get("estado"), estado));
            if (sucursalFinal != null) condiciones.add(cb.equal(root.get("sucursalRecojo"), sucursalFinal));
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        int paginaPedida = Math.max(0, pagina == null ? 0 : pagina);
        int tamanoPedido = tamanoValido(tamano);
        Page<Reserva> resultado = reservas.findAll(filtros, PageRequest.of(paginaPedida, tamanoPedido,
                Sort.by(Sort.Order.desc("fechaInicio"), Sort.Order.desc("creadoEn"))));
        return new PaginaReservasResponse(resultado.getTotalElements(), paginaPedida, tamanoPedido,
                resultado.getTotalPages(), resultado.getContent().stream().map(ReservaResponse::de).toList());
    }

    /** Historial de cambios de una reserva, del más reciente al más antiguo. Solo personal y servicios. */
    public List<HistorialReservaResponse> consultarHistorial(UUID id, Actor actor) {
        String rol = actor.rolNormalizado();
        if (!ROLES_PERSONAL.contains(rol) && !ROL_SERVICIO.equals(rol)) {
            throw ReservaException.rolNoAutorizado("Solo el personal puede ver el historial de una reserva");
        }
        autorizarLectura(cargar(id), actor);
        return historial.findByIdReservaOrderByFechaDesc(id).stream().map(HistorialReservaResponse::de).toList();
    }

    // ---------------------------------------------------------------- modificar

    /** Cambia fechas y/o categoría (solo CONFIRMADA) o el estado (por ejemplo EN_CURSO en el check-out). */
    public ReservaResponse modificar(UUID id, ModificarReservaRequest cambios, Actor actor) {
        Reserva reserva = cargar(id);
        // Registrar la devolución (FINALIZADA) también lo puede hacer la sede donde se devuelve el vehículo.
        autorizarEscritura(reserva, actor, cambios.estado() == EstadoReserva.FINALIZADA);

        boolean solicitaEstado = cambios.estado() != null;
        boolean cambiaDatos = cambios.categoria() != null || cambios.fechaInicio() != null || cambios.fechaFin() != null;
        if (solicitaEstado && cambiaDatos) {
            throw ReservaException.solicitudInvalida("Cambia las fechas/categoria o el estado, pero no ambos a la vez");
        }
        if (solicitaEstado) {
            return cambiarEstado(reserva, cambios.estado(), cambios.motivo(), actor);
        }
        if (!cambiaDatos) {
            return ReservaResponse.de(reserva);   // nada que cambiar
        }
        return reprogramar(reserva, cambios, actor);
    }

    public ReservaResponse cancelar(UUID id, String motivo, Actor actor) {
        Reserva reserva = cargar(id);
        autorizarEscritura(reserva, actor, false);
        return cancelarReserva(reserva, motivo, actor);
    }

    private ReservaResponse cambiarEstado(Reserva reserva, EstadoReserva nuevo, String motivo, Actor actor) {
        String rol = actor.rolNormalizado();
        if (nuevo == EstadoReserva.CANCELADA) {
            return cancelarReserva(reserva, motivo, actor);
        }
        if (ROL_CLIENTE.equals(rol)) {
            throw ReservaException.rolNoAutorizado("El cliente solo puede cancelar su reserva");
        }
        if (reserva.getEstado() == nuevo) {
            return sincronizarContrato(reserva, nuevo);
        }
        if (!reserva.getEstado().puedePasarA(nuevo)) {
            throw ReservaException.transicionInvalida(
                    "Una reserva " + reserva.getEstado() + " no puede pasar a " + nuevo);
        }
        EstadoReserva anterior = reserva.getEstado();
        if (nuevo == EstadoReserva.EN_CURSO) {
            // Según el documento, en el check-out el vehículo pasa a ALQUILADO en flota ANTES del contrato.
            // Se hace antes de tocar la reserva: si el vehículo no se puede entregar (por ejemplo, está en
            // mantenimiento) la reserva queda como estaba y el agente recibe un mensaje claro.
            entregarVehiculo(reserva);
        }
        reserva.cambiarEstado(nuevo);
        ReservaResponse respuesta = guardarConHistorial(reserva, nuevo.name(), anterior, rol, actor, motivo);
        if (nuevo == EstadoReserva.FINALIZADA) {
            liberarSinFallar(reserva.getId());   // el vehículo volvió: que quede libre para otras reservas
            recibirVehiculo(reserva);            // y en flota deja de figurar como ALQUILADO
        }
        if (nuevo == EstadoReserva.EN_CURSO) {
            contratos.crear(reserva.getId());
        } else if (nuevo == EstadoReserva.FINALIZADA) {
            contratos.finalizar(reserva.getId());
        }
        return respuesta;
    }

    // Reintento de una entrega o devolución que quedó a medias (la reserva ya cambió de estado pero falló
    // un paso posterior). Todos los pasos son idempotentes, así que repetirlos es seguro.
    private ReservaResponse sincronizarContrato(Reserva reserva, EstadoReserva estado) {
        if (estado == EstadoReserva.EN_CURSO) {
            entregarVehiculo(reserva);
            contratos.crear(reserva.getId());
        } else if (estado == EstadoReserva.FINALIZADA) {
            liberarSinFallar(reserva.getId());
            recibirVehiculo(reserva);
            contratos.finalizar(reserva.getId());
        }
        return ReservaResponse.de(reserva);
    }

    private void entregarVehiculo(Reserva reserva) {
        if (reserva.getPlacaAsignada() != null) {
            flota.marcarAlquilado(reserva.getPlacaAsignada(), "Check-out de la reserva " + reserva.getId());
        }
    }

    private void recibirVehiculo(Reserva reserva) {
        if (reserva.getPlacaAsignada() != null) {
            flota.marcarDevuelto(reserva.getPlacaAsignada(), "Devolución de la reserva " + reserva.getId());
        }
    }

    private ReservaResponse reprogramar(Reserva reserva, ModificarReservaRequest cambios, Actor actor) {
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw ReservaException.transicionInvalida(
                    "Solo se pueden modificar reservas CONFIRMADAS (esta esta " + reserva.getEstado() + ")");
        }
        String categoria = cambios.categoria() == null ? reserva.getCategoria() : categoriaValida(cambios.categoria());
        LocalDate inicio = cambios.fechaInicio() == null ? reserva.getFechaInicio() : cambios.fechaInicio();
        LocalDate fin = cambios.fechaFin() == null ? reserva.getFechaFin() : cambios.fechaFin();
        validarFechas(inicio, fin, !inicio.equals(reserva.getFechaInicio()));

        if (categoria.equals(reserva.getCategoria()) && inicio.equals(reserva.getFechaInicio())
                && fin.equals(reserva.getFechaFin())) {
            return ReservaResponse.de(reserva);   // mismos valores: no hay nada que reprogramar
        }

        // Si se alarga el alquiler, la licencia debe seguir vigente hasta la nueva fecha de fin.
        if (!fin.equals(reserva.getFechaFin())) {
            exigirLicenciaVigente(clientes.obtener(reserva.getIdCliente()), fin);
        }

        String categoriaAnterior = reserva.getCategoria();
        LocalDate inicioAnterior = reserva.getFechaInicio();
        LocalDate finAnterior = reserva.getFechaFin();
        String detalle = describirCambio(categoriaAnterior, inicioAnterior, finAnterior, categoria, inicio, fin, cambios.motivo());

        // Flota cambia el apartado de forma atómica: si no hay cupo, la reserva conserva su vehículo.
        var apartado = flota.reprogramar(reserva.getId().toString(), reserva.getSucursalRecojo(), categoria, inicio, fin);
        reserva.reprogramar(categoria, inicio, fin, apartado.placa());
        try {
            return guardarConHistorial(reserva, "MODIFICADA", reserva.getEstado(), actor.rolNormalizado(), actor, detalle);
        } catch (RuntimeException ex) {
            try {   // compensación: devolver el apartado a como estaba
                flota.reprogramar(reserva.getId().toString(), reserva.getSucursalRecojo(),
                        categoriaAnterior, inicioAnterior, finAnterior);
            } catch (RuntimeException comp) {
                log.error("No se pudo revertir el apartado de la reserva {}", reserva.getId(), comp);
            }
            throw ex;
        }
    }

    /**
     * Cancela y libera el vehículo. Es idempotente: si ya estaba CANCELADA solo reintenta la
     * liberación, así que si flota falló la primera vez basta con volver a cancelar.
     */
    private ReservaResponse cancelarReserva(Reserva reserva, String motivo, Actor actor) {
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            liberarObligatorio(reserva.getId());
            return ReservaResponse.de(reserva);
        }
        if (!reserva.getEstado().puedePasarA(EstadoReserva.CANCELADA)) {
            throw ReservaException.transicionInvalida(
                    "No se puede cancelar una reserva " + reserva.getEstado());
        }
        EstadoReserva anterior = reserva.getEstado();
        reserva.cambiarEstado(EstadoReserva.CANCELADA);
        ReservaResponse respuesta = guardarConHistorial(reserva, "CANCELADA", anterior,
                actor.rolNormalizado(), actor, motivo);
        liberarObligatorio(reserva.getId());
        return respuesta;
    }

    // ---------------------------------------------------------------- utilidades

    // Guarda la reserva y su registro de historial en una sola transacción: o los dos o ninguno.
    private ReservaResponse guardarConHistorial(Reserva reserva, String accion, EstadoReserva anterior,
                                                String rol, Actor actor, String detalle) {
        return tx.execute(estado -> {
            Reserva guardada = reservas.save(reserva);
            historial.save(new HistorialReserva(guardada.getId(), accion, anterior, guardada.getEstado(),
                    actor.usuarioONombre(), rol, esVacio(detalle) ? null : detalle.trim()));
            return ReservaResponse.de(guardada);
        });
    }

    private void liberarObligatorio(UUID id) {
        try {
            flota.liberar(id.toString());
        } catch (ReservaException ex) {
            throw new ReservaException("SERVICIO_NO_DISPONIBLE", ex.getStatus(),
                    "La reserva quedo CANCELADA, pero no se pudo liberar el vehiculo. "
                            + "Vuelve a cancelarla para completar la liberacion");
        }
    }

    private void liberarSinFallar(UUID id) {
        try {
            flota.liberar(id.toString());
        } catch (RuntimeException ex) {
            log.error("No se pudo liberar el vehiculo apartado de la reserva {}", id, ex);
        }
    }

    private Reserva cargar(UUID id) {
        return reservas.findById(id).orElseThrow(ReservaException::noEncontrada);
    }

    // Lectura: el cliente solo ve sus reservas (si no es suya, "no existe"); el personal de sucursal solo
    // las que salen o llegan a su sede (si no, también "no existe"); el administrador general ve todas.
    private void autorizarLectura(Reserva reserva, Actor actor) {
        String rol = actor.rolNormalizado();
        if (ROL_CLIENTE.equals(rol)) {
            exigirDuenio(reserva, actor);
        } else if (esPersonalDeSucursal(rol)) {
            if (!esDeSuSede(actor, reserva.getSucursalRecojo()) && !esDeSuSede(actor, reserva.getSucursalDevolucion())) {
                throw ReservaException.noEncontrada();
            }
        } else if (!ROL_GENERAL.equals(rol) && !ROL_SERVICIO.equals(rol)) {
            throw ReservaException.rolNoAutorizado("Tu rol no puede consultar reservas");
        }
    }

    // Escritura: el personal de sucursal solo gestiona reservas cuyo recojo es su sede; con "incluyeDevolucion"
    // también las que se devuelven en su sede (para registrar la devolución del vehículo).
    private void autorizarEscritura(Reserva reserva, Actor actor, boolean incluyeDevolucion) {
        String rol = actor.rolNormalizado();
        if (ROL_CLIENTE.equals(rol)) {
            exigirDuenio(reserva, actor);
        } else if (ROL_SERVICIO.equals(rol) || ROL_GENERAL.equals(rol)) {
            return;
        } else if (ROLES_PERSONAL.contains(rol)) {
            boolean permitido = esDeSuSede(actor, reserva.getSucursalRecojo())
                    || (incluyeDevolucion && esDeSuSede(actor, reserva.getSucursalDevolucion()));
            if (!permitido) {
                // Si ni siquiera puede verla, se responde como si no existiera.
                if (!esDeSuSede(actor, reserva.getSucursalDevolucion())) {
                    throw ReservaException.noEncontrada();
                }
                throw ReservaException.sucursalNoAutorizada(reserva.getSucursalRecojo());
            }
        } else {
            throw ReservaException.rolNoAutorizado("Tu rol no puede modificar reservas");
        }
    }

    // AGENTE y ADMINISTRADOR_SUCURSAL: el personal atado a una sede (el ADMINISTRADOR general no lo es).
    private static boolean esPersonalDeSucursal(String rol) {
        return ROLES_PERSONAL.contains(rol) && !ROL_GENERAL.equals(rol);
    }

    private static boolean esDeSuSede(Actor actor, String sucursal) {
        return !esVacio(actor.sucursal()) && actor.sucursal().trim().equalsIgnoreCase(sucursal);
    }

    // El cliente necesita una licencia registrada y vigente hasta el último día del alquiler.
    private void exigirLicenciaVigente(ClienteInfo cliente, LocalDate fechaFin) {
        if (esVacio(cliente.licenciaConducir()) || cliente.vencimientoLicencia() == null) {
            throw ReservaException.licenciaNoVigente(
                    "El cliente no tiene licencia de conducir registrada; actualiza sus datos antes de reservar");
        }
        if (cliente.vencimientoLicencia().isBefore(fechaFin)) {
            throw ReservaException.licenciaNoVigente("La licencia del cliente vence el "
                    + cliente.vencimientoLicencia() + ", antes del fin del alquiler (" + fechaFin + ")");
        }
    }

    private void exigirDuenio(Reserva reserva, Actor actor) {
        UUID idPropio = clientes.porCorreo(actor.usuario()).idCliente();
        if (!idPropio.equals(reserva.getIdCliente())) {
            throw ReservaException.noEncontrada();   // no se revela que existe
        }
    }

    private ClienteInfo resolverCliente(UUID idSolicitado, Actor actor, String rol) {
        if (ROL_CLIENTE.equals(rol)) {
            return clientes.porCorreo(actor.usuario());
        }
        if (idSolicitado == null) {
            throw ReservaException.solicitudInvalida("idCliente es obligatorio");
        }
        return clientes.obtener(idSolicitado);
    }

    private String categoriaValida(String categoria) {
        String normalizada = categoria == null ? "" : categoria.trim().toUpperCase();
        if (!CATEGORIAS.contains(normalizada)) {
            throw ReservaException.solicitudInvalida("categoria invalida; usa " + String.join(", ", CATEGORIAS));
        }
        return normalizada;
    }

    private String sucursalValida(String sucursal, String campo) {
        String normalizada = sucursal == null ? "" : sucursal.trim().toUpperCase();
        if (!SUCURSALES.contains(normalizada)) {
            throw ReservaException.solicitudInvalida(campo + " invalida; usa " + String.join(", ", SUCURSALES));
        }
        return normalizada;
    }

    private void validarFechas(LocalDate inicio, LocalDate fin, boolean exigirNoPasada) {
        if (fin.isBefore(inicio)) {
            throw ReservaException.solicitudInvalida("fechaFin no puede ser anterior a fechaInicio");
        }
        if (exigirNoPasada && inicio.isBefore(LocalDate.now(reloj))) {
            throw ReservaException.solicitudInvalida("fechaInicio no puede ser anterior a hoy");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > maxDias) {
            throw ReservaException.solicitudInvalida("Una reserva no puede durar mas de " + maxDias + " dias");
        }
    }

    private static String describirCambio(String catAntes, LocalDate iniAntes, LocalDate finAntes,
                                          String catAhora, LocalDate iniAhora, LocalDate finAhora, String motivo) {
        StringBuilder sb = new StringBuilder();
        if (!catAntes.equals(catAhora)) sb.append("categoria ").append(catAntes).append("->").append(catAhora).append("; ");
        if (!iniAntes.equals(iniAhora) || !finAntes.equals(finAhora)) {
            sb.append("fechas ").append(iniAntes).append("/").append(finAntes)
                    .append("->").append(iniAhora).append("/").append(finAhora).append("; ");
        }
        if (!esVacio(motivo)) sb.append("motivo: ").append(motivo.trim());
        return sb.toString().trim();
    }

    private static int tamanoValido(Integer tamano) {
        return tamano == null ? TAMANO_POR_DEFECTO : Math.min(TAMANO_MAXIMO, Math.max(1, tamano));
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }
}
