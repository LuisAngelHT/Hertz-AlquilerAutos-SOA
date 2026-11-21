package com.hertz.contrato.service;

import com.hertz.contrato.client.ReservaClient;
import com.hertz.contrato.client.ReservaInfo;
import com.hertz.contrato.dto.ContratoResponse;
import com.hertz.contrato.dto.DanoRequest;
import com.hertz.contrato.dto.DevolucionRequest;
import com.hertz.contrato.dto.DocumentoMeta;
import com.hertz.contrato.dto.DocumentoResponse;
import com.hertz.contrato.dto.InspeccionRequest;
import com.hertz.contrato.dto.InspeccionResponse;
import com.hertz.contrato.dto.ItemInventarioRequest;
import com.hertz.contrato.dto.PaginaContratosResponse;
import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.ContratoDano;
import com.hertz.contrato.entity.ContratoDocumento;
import com.hertz.contrato.entity.ContratoInspeccion;
import com.hertz.contrato.entity.ContratoItem;
import com.hertz.contrato.entity.EstadoContrato;
import com.hertz.contrato.entity.TipoInspeccion;
import com.hertz.contrato.exception.ContratoException;
import com.hertz.contrato.repository.ContratoDocumentoRepository;
import com.hertz.contrato.repository.ContratoInspeccionRepository;
import com.hertz.contrato.repository.ContratoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ContratoService {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_SERVICIO = "SERVICIO";
    private static final Set<String> ROLES_PERSONAL =
            Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL");
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final int TAMANO_MAXIMO = 50;
    /** Maximo del contrato escaneado (debe coincidir con el limite del SQL y de application.yml). */
    static final int DOCUMENTO_MAX_BYTES = 5 * 1024 * 1024;

    private final ContratoRepository repository;
    private final ReservaClient reservaClient;
    private final ContratoDocumentoRepository documentos;
    private final ContratoInspeccionRepository inspecciones;
    private final TransactionTemplate transacciones;

    public ContratoService(ContratoRepository repository, ReservaClient reservaClient,
                           ContratoDocumentoRepository documentos, ContratoInspeccionRepository inspecciones,
                           PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.reservaClient = reservaClient;
        this.documentos = documentos;
        this.inspecciones = inspecciones;
        this.transacciones = new TransactionTemplate(transactionManager);
    }

    /**
     * Crea el contrato al entregar el vehículo. ServicioReserva proporciona la reserva y solo se crea cuando
     * esta se encuentra EN_CURSO. La entrega exige dos cosas: la revisión del vehículo al salir (kilometraje,
     * combustible y daños que ya tenía, para que el cliente no cargue con ellos después) y la confirmación
     * de que el contrato impreso fue firmado. Se puede repetir: si el contrato ya existe, lo devuelve.
     */
    public ContratoResponse crear(UUID idReserva, InspeccionRequest entrega, Boolean contratoFirmado,
                                  String rol, String sucursal, String usuario) {
        exigirAcceso(rol);

        var existente = repository.findByIdReserva(idReserva);
        if (existente.isPresent()) {
            Contrato contrato = existente.get();
            autorizarSucursal(rol, sucursal, contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
            return responder(contrato);
        }

        if (entrega == null) {
            throw ContratoException.revisionInvalida("La revisión del vehículo al entregarlo es obligatoria");
        }
        if (!Boolean.TRUE.equals(contratoFirmado)) {
            throw ContratoException.contratoNoFirmado();
        }

        ReservaInfo reserva = reservaClient.obtener(idReserva);
        if (!"EN_CURSO".equals(reserva.estado())) {
            throw ContratoException.reservaNoDisponible(
                    "Solo se puede crear un contrato para una reserva EN_CURSO");
        }
        if (reserva.placaAsignada() == null || reserva.idCliente() == null) {
            throw ContratoException.reservaNoDisponible(
                    "La reserva debe tener un cliente y un vehículo asignado");
        }
        autorizarSucursal(rol, sucursal, reserva.sucursalRecojo(), reserva.sucursalDevolucion());

        String registradaPor = limpiarUsuario(usuario);

        // El inicio del contrato es el día REAL de la entrega (hoy), no la fecha de la reserva: si el cliente
        // se lleva el vehículo antes o después de lo reservado, el contrato refleja cuándo salió de verdad.
        // (Si ya pasó el fin previsto, se acota a ese día: el inicio nunca puede ser posterior al fin.)
        LocalDate hoy = LocalDate.now(ZONA_LIMA);
        LocalDate inicioReal = hoy.isAfter(reserva.fechaFin()) ? reserva.fechaFin() : hoy;
        Contrato contrato = new Contrato(
                UUID.randomUUID(),
                reserva.idReserva(),
                reserva.idCliente(),
                reserva.placaAsignada(),
                reserva.sucursalRecojo(),
                reserva.sucursalDevolucion(),
                inicioReal,
                reserva.fechaFin(),
                registradaPor);
        // Los daños de la entrega son los que el vehículo YA tenía: no llevan costo (no se le cobran a nadie).
        ContratoInspeccion revision = new ContratoInspeccion(contrato.getId(), TipoInspeccion.ENTREGA,
                entrega.kilometraje(), entrega.combustible(), limpiarTexto(entrega.observaciones()), null,
                registradaPor, aDanos(entrega.danosONinguno(), false),
                aItems(entrega.inventarioONinguno(), false));
        try {
            // Contrato y revisión se guardan juntos: no puede existir un contrato sin su revisión de salida.
            return transacciones.execute(estado -> {
                repository.saveAndFlush(contrato);
                inspecciones.saveAndFlush(revision);
                return ContratoResponse.de(contrato, null, InspeccionResponse.de(revision), null);
            });
        } catch (DataIntegrityViolationException ex) {
            return repository.findByIdReserva(idReserva)
                    .map(this::responder)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public PaginaContratosResponse listar(
            UUID idReserva,
            UUID idCliente,
            String placa,
            EstadoContrato estado,
            int pagina,
            int tamano,
            String rol,
            String sucursal) {
        exigirAcceso(rol);
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.min(Math.max(1, tamano), TAMANO_MAXIMO);

        Specification<Contrato> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idReserva != null) condiciones.add(cb.equal(root.get("idReserva"), idReserva));
            if (idCliente != null) condiciones.add(cb.equal(root.get("idCliente"), idCliente));
            if (placa != null && !placa.isBlank()) {
                condiciones.add(cb.equal(root.get("placa"), placa.trim().toUpperCase()));
            }
            if (estado != null) condiciones.add(cb.equal(root.get("estado"), estado));
            if (requiereSucursal(rol)) {
                if (sucursal == null || sucursal.isBlank()) {
                    throw ContratoException.sucursalNoAutorizada();
                }
                String sede = sucursal.trim().toUpperCase();
                condiciones.add(cb.or(
                        cb.equal(root.get("sucursalRecojo"), sede),
                        cb.equal(root.get("sucursalDevolucion"), sede)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        Page<Contrato> resultado = repository.findAll(
                filtro,
                PageRequest.of(paginaSegura, tamanoSeguro,
                        Sort.by(Sort.Direction.DESC, "creadoEn")));
        return new PaginaContratosResponse(
                resultado.getTotalElements(),
                paginaSegura,
                tamanoSeguro,
                resultado.getTotalPages(),
                responderTodos(resultado.getContent()));
    }

    @Transactional(readOnly = true)
    public ContratoResponse obtener(UUID id, String rol, String sucursal) {
        exigirAcceso(rol);
        Contrato contrato = repository.findById(id)
                .orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal,
                contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
        return responder(contrato);
    }

    @Transactional(readOnly = true)
    public ContratoResponse obtenerPorReserva(UUID idReserva, String rol, String sucursal) {
        exigirAcceso(rol);
        Contrato contrato = repository.findByIdReserva(idReserva)
                .orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal,
                contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
        return responder(contrato);
    }

    /**
     * Finaliza el contrato al recibir el vehículo, con la revisión de devolución. Los daños de esa revisión son
     * los nuevos (causados durante el alquiler) y quedan a cargo del cliente. Se puede repetir de forma segura.
     */
    public ContratoResponse finalizarPorReserva(
            UUID idReserva, DevolucionRequest devolucion, String rol, String sucursal, String usuario) {
        exigirAcceso(rol);
        Contrato contrato = repository.findByIdReserva(idReserva)
                .orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal,
                contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());

        if (contrato.getEstado() == EstadoContrato.FINALIZADO) {
            return responder(contrato);
        }

        if (devolucion == null) {
            throw ContratoException.revisionInvalida("La revisión del vehículo al recibirlo es obligatoria");
        }
        // Contratos anteriores a esta función no tienen revisión de entrega: ahí no hay con qué comparar.
        ContratoInspeccion salida = inspecciones.findByIdContratoIn(List.of(contrato.getId())).stream()
                .filter(i -> i.getTipo() == TipoInspeccion.ENTREGA)
                .findFirst()
                .orElse(null);
        if (salida != null && devolucion.kilometraje() < salida.getKilometraje()) {
            throw ContratoException.revisionInvalida("El kilometraje al devolver (" + devolucion.kilometraje()
                    + ") no puede ser menor al de la entrega (" + salida.getKilometraje() + ")");
        }
        boolean conforme = Boolean.TRUE.equals(devolucion.clienteConforme());
        if (!conforme && limpiarTexto(devolucion.observaciones()) == null) {
            throw ContratoException.revisionInvalida(
                    "Si el cliente no está conforme, explica el motivo en las observaciones");
        }

        ReservaInfo reserva = reservaClient.obtener(idReserva);
        // La reserva pasa a FINALIZADA justo después de que el contrato valida y cierra la devolución
        // (así un dato incorrecto se rechaza antes de cambiar nada), por eso también se acepta EN_CURSO.
        if (!"FINALIZADA".equals(reserva.estado()) && !"EN_CURSO".equals(reserva.estado())) {
            throw ContratoException.transicionInvalida(
                    "La reserva debe estar EN_CURSO o FINALIZADA para cerrar el contrato");
        }

        // La devolución se compara con el momento REAL de la entrega (cuando se creó el contrato), no con la
        // fecha de la reserva: se puede devolver el mismo día, incluso a las pocas horas, y también cuando la
        // reserva era para más adelante y se entregó antes.
        LocalDate entregaReal = contrato.getCreadoEn().atZone(ZONA_LIMA).toLocalDate();
        LocalDate fechaDevolucion = LocalDate.now(ZONA_LIMA);
        if (fechaDevolucion.isBefore(entregaReal)) {
            throw ContratoException.transicionInvalida(
                    "La devolución no puede ser anterior a la entrega del vehículo (" + entregaReal + ")");
        }
        contrato.ajustarInicioA(entregaReal);   // también corrige contratos que quedaron con la fecha de la reserva
        ContratoInspeccion revision = new ContratoInspeccion(contrato.getId(), TipoInspeccion.DEVOLUCION,
                devolucion.kilometraje(), devolucion.combustible(), limpiarTexto(devolucion.observaciones()),
                conforme, limpiarUsuario(usuario), aDanos(devolucion.danosONinguno(), true),
                aItems(devolucion.inventarioONinguno(), true));
        try {
            // La revisión y el cierre del contrato van juntos: no queda un contrato cerrado sin su revisión.
            transacciones.executeWithoutResult(estado -> {
                inspecciones.saveAndFlush(revision);
                contrato.finalizar(fechaDevolucion);
                repository.save(contrato);
            });
        } catch (DataIntegrityViolationException ex) {
            // Otra solicitud lo cerró al mismo tiempo: se devuelve el contrato como quedó.
            return repository.findByIdReserva(idReserva)
                    .filter(c -> c.getEstado() == EstadoContrato.FINALIZADO)
                    .map(this::responder)
                    .orElseThrow(() -> ex);
        }
        ContratoResponse base = responder(contrato);   // trae el escaneo y la revision de entrega
        return ContratoResponse.de(contrato, base.documento(), base.entrega(), InspeccionResponse.de(revision));
    }

    // ---------------------------------------------------------------- contrato firmado escaneado

    /**
     * Guarda el contrato firmado y escaneado (PDF, JPG o PNG, maximo 5 MB). Si ya habia uno lo reemplaza.
     * Solo el personal de Hertz, y solo en contratos de su sucursal (el administrador general, en todos).
     */
    @Transactional
    public ContratoResponse subirDocumento(UUID id, String rol, String sucursal, String usuario,
                                           String nombreOriginal, byte[] contenido) {
        exigirPersonal(rol);
        Contrato contrato = repository.findById(id).orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal, contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());

        if (contenido == null || contenido.length == 0) {
            throw ContratoException.documentoInvalido("El archivo está vacío");
        }
        if (contenido.length > DOCUMENTO_MAX_BYTES) {
            throw ContratoException.documentoMuyGrande();
        }
        String tipo = detectarTipo(contenido);
        if (tipo == null) {
            throw ContratoException.documentoInvalido("Solo se aceptan archivos PDF, JPG o PNG");
        }
        String nombre = nombreSeguro(nombreOriginal, tipo);
        String subidoPor = usuario == null || usuario.isBlank() ? "DESCONOCIDO" : usuario.trim();
        if (subidoPor.length() > 120) {
            subidoPor = subidoPor.substring(0, 120);
        }

        ContratoDocumento documento = documentos.findById(id).orElse(null);
        if (documento == null) {
            documento = new ContratoDocumento(id, nombre, tipo, contenido, subidoPor);
        } else {
            documento.reemplazar(nombre, tipo, contenido, subidoPor);
        }
        DocumentoResponse guardado = DocumentoResponse.de(documentos.saveAndFlush(documento));
        ContratoResponse base = responder(contrato);   // trae las revisiones del contrato
        return ContratoResponse.de(contrato, guardado, base.entrega(), base.devolucion());
    }

    /** Devuelve el archivo para verlo o descargarlo. Mismas reglas de acceso que al subirlo. */
    @Transactional(readOnly = true)
    public ContratoDocumento obtenerDocumento(UUID id, String rol, String sucursal) {
        exigirPersonal(rol);
        Contrato contrato = repository.findById(id).orElseThrow(ContratoException::contratoNoEncontrado);
        autorizarSucursal(rol, sucursal, contrato.getSucursalRecojo(), contrato.getSucursalDevolucion());
        return documentos.findById(id).orElseThrow(ContratoException::documentoNoEncontrado);
    }

    /** El tipo se deduce del contenido real (no del nombre ni de lo que declare el navegador). */
    static String detectarTipo(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return "application/pdf";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }

    /** Nombre limpio para guardar: sin rutas ni caracteres raros, y con la extension que corresponde al tipo real. */
    static String nombreSeguro(String original, String tipo) {
        String extension = switch (tipo) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            default -> ".png";
        };
        String base = original == null ? "" : original;
        base = base.substring(Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\')) + 1);
        int punto = base.lastIndexOf('.');
        if (punto > 0 && base.length() - punto <= 5) {
            base = base.substring(0, punto);
        }
        base = base.replaceAll("[^\\p{L}\\p{N} ._()-]", "_").trim();
        if (base.isBlank()) {
            base = "contrato-firmado";
        }
        if (base.length() > 120) {
            base = base.substring(0, 120);
        }
        return base + extension;
    }

    private ContratoResponse responder(Contrato contrato) {
        return responderTodos(List.of(contrato)).get(0);
    }

    /** Arma las respuestas con el contrato firmado y las revisiones: una consulta por tipo para toda la pagina. */
    private List<ContratoResponse> responderTodos(List<Contrato> contratos) {
        if (contratos.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = contratos.stream().map(Contrato::getId).toList();
        Map<UUID, DocumentoResponse> escaneos = new HashMap<>();
        documentos.metasPorContratos(ids).forEach(meta -> escaneos.put(meta.idContrato(), meta.respuesta()));
        Map<UUID, InspeccionResponse> entregas = new HashMap<>();
        Map<UUID, InspeccionResponse> devoluciones = new HashMap<>();
        for (ContratoInspeccion revision : inspecciones.findByIdContratoIn(ids)) {
            (revision.getTipo() == TipoInspeccion.ENTREGA ? entregas : devoluciones)
                    .put(revision.getIdContrato(), InspeccionResponse.de(revision));
        }
        return contratos.stream()
                .map(c -> ContratoResponse.de(c, escaneos.get(c.getId()), entregas.get(c.getId()),
                        devoluciones.get(c.getId())))
                .toList();
    }

    private static List<ContratoDano> aDanos(List<DanoRequest> danos, boolean conCosto) {
        return danos.stream()
                .map(d -> {
                    // Un dano marcado en el dibujo necesita vista Y posicion; sin vista no se guarda ninguna posicion.
                    if (d.vista() != null && (d.posX() == null || d.posY() == null)) {
                        throw ContratoException.revisionInvalida("Un daño marcado en el dibujo necesita su posición");
                    }
                    boolean ubicado = d.vista() != null;
                    return new ContratoDano(d.zona(), d.descripcion().trim(), d.gravedad(),
                            conCosto ? d.costoEstimado() : null, d.vista(),
                            ubicado ? d.posX() : null, ubicado ? d.posY() : null, d.tipo());
                })
                .toList();
    }

    /** Inventario de la revision. Cada elemento va una sola vez; los costos solo cuentan en la devolucion. */
    private static List<ContratoItem> aItems(List<ItemInventarioRequest> items, boolean conCosto) {
        java.util.Set<com.hertz.contrato.entity.ItemInventario> vistos = new java.util.HashSet<>();
        for (ItemInventarioRequest item : items) {
            if (!vistos.add(item.item())) {
                throw ContratoException.revisionInvalida("El elemento " + item.item() + " está repetido en el inventario");
            }
        }
        return items.stream()
                .map(i -> new ContratoItem(i.item(), i.estado(), conCosto ? i.costoEstimado() : null))
                .toList();
    }

    private static String limpiarUsuario(String usuario) {
        String limpio = usuario == null || usuario.isBlank() ? "DESCONOCIDO" : usuario.trim();
        return limpio.length() > 120 ? limpio.substring(0, 120) : limpio;
    }

    private static String limpiarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private void exigirPersonal(String rol) {
        String normalizado = rol == null ? "" : rol.trim().toUpperCase();
        if (!ROLES_PERSONAL.contains(normalizado)) {
            throw ContratoException.rolNoAutorizado();
        }
    }

    private void exigirAcceso(String rol) {
        String normalizado = rol == null ? "" : rol.trim().toUpperCase();
        if (!ROLES_PERSONAL.contains(normalizado) && !ROL_SERVICIO.equals(normalizado)) {
            throw ContratoException.rolNoAutorizado();
        }
    }

    private boolean requiereSucursal(String rol) {
        String normalizado = rol == null ? "" : rol.trim().toUpperCase();
        return !ROL_ADMINISTRADOR.equals(normalizado) && !ROL_SERVICIO.equals(normalizado);
    }

    private void autorizarSucursal(
            String rol, String sucursalUsuario, String sucursalRecojo, String sucursalDevolucion) {
        if (!requiereSucursal(rol)) return;
        if (sucursalUsuario == null || sucursalUsuario.isBlank()) {
            throw ContratoException.sucursalNoAutorizada();
        }
        String sede = sucursalUsuario.trim();
        if (!sede.equalsIgnoreCase(sucursalRecojo)
                && !sede.equalsIgnoreCase(sucursalDevolucion)) {
            throw ContratoException.sucursalNoAutorizada();
        }
    }
}
