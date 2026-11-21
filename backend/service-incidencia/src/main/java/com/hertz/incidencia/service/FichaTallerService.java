package com.hertz.incidencia.service;

import com.hertz.incidencia.dto.CierreRequest;
import com.hertz.incidencia.dto.DanoExternoRequest;
import com.hertz.incidencia.dto.DanoInternoRequest;
import com.hertz.incidencia.dto.FichaRequest;
import com.hertz.incidencia.dto.FichaResponse;
import com.hertz.incidencia.dto.IncidenciaResponse;
import com.hertz.incidencia.dto.PaginaIncidenciasResponse;
import com.hertz.incidencia.dto.SeguimientoRequest;
import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.entity.FichaDanoExterno;
import com.hertz.incidencia.entity.FichaDanoInterno;
import com.hertz.incidencia.entity.FichaSeguimiento;
import com.hertz.incidencia.entity.FichaTaller;
import com.hertz.incidencia.entity.Incidencia;
import com.hertz.incidencia.exception.IncidenciaException;
import com.hertz.incidencia.repository.FichaDanoExternoRepository;
import com.hertz.incidencia.repository.FichaDanoInternoRepository;
import com.hertz.incidencia.repository.FichaSeguimientoRepository;
import com.hertz.incidencia.repository.FichaTallerRepository;
import com.hertz.incidencia.repository.IncidenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ficha tecnica del taller de una incidencia: lo que el mecanico diagnostica y registra mientras el vehiculo esta en
 * MANTENIMIENTO (danos externos e internos, dias en taller, costos y avances) y lo que deja al cerrar.
 */
@Service
public class FichaTallerService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final IncidenciaRepository incidencias;
    private final FichaTallerRepository fichas;
    private final FichaDanoInternoRepository internos;
    private final FichaDanoExternoRepository externos;
    private final FichaSeguimientoRepository seguimientos;

    public FichaTallerService(IncidenciaRepository incidencias, FichaTallerRepository fichas,
                              FichaDanoInternoRepository internos, FichaDanoExternoRepository externos,
                              FichaSeguimientoRepository seguimientos) {
        this.incidencias = incidencias;
        this.fichas = fichas;
        this.internos = internos;
        this.externos = externos;
        this.seguimientos = seguimientos;
    }

    @Transactional(readOnly = true)
    public FichaResponse obtener(UUID id, String sucursalUsuario) {
        return armar(cargarAccesible(id, sucursalUsuario));
    }

    /** Guarda el diagnostico, el plan y las listas de danos. Solo mientras la incidencia esta abierta. */
    @Transactional
    public FichaResponse guardar(UUID id, FichaRequest pedido, String usuario, String sucursalUsuario) {
        Incidencia incidencia = cargarAccesible(id, sucursalUsuario);
        exigirAbierta(incidencia);
        String quien = limpiarUsuario(usuario);

        // La fecha de salida prevista cuenta desde que el vehiculo entro a taller (cuando se registro la incidencia).
        LocalDate ingreso = ingreso(incidencia);
        Integer dias = pedido.diasEstimados();
        LocalDate salida = dias == null ? null : ingreso.plusDays(dias);

        FichaTaller ficha = fichas.findById(id).orElseGet(() -> new FichaTaller(id, quien));
        ficha.actualizarPlan(limpiarTexto(pedido.diagnostico()), dias, salida,
                pedido.costoRepuestos(), pedido.costoManoObra(), quien);
        fichas.save(ficha);

        // Las listas se reescriben completas (lo que no viene, se quita).
        internos.deleteByIdIncidencia(id);
        externos.deleteByIdIncidencia(id);
        internos.flush();
        externos.flush();
        int orden = 0;
        for (DanoInternoRequest d : pedido.internosONinguno()) {
            internos.save(new FichaDanoInterno(id, orden++, d.sistema(), d.descripcion().trim(), d.gravedad(), d.costoEstimado()));
        }
        orden = 0;
        for (DanoExternoRequest d : pedido.externosONinguno()) {
            externos.save(new FichaDanoExterno(id, orden++, d.vista(), d.posX(), d.posY(), d.tipo(),
                    d.descripcion().trim(), d.gravedad(), d.costoEstimado()));
        }
        return armar(incidencia);
    }

    @Transactional
    public FichaResponse agregarSeguimiento(UUID id, SeguimientoRequest pedido, String usuario, String sucursalUsuario) {
        Incidencia incidencia = cargarAccesible(id, sucursalUsuario);
        exigirAbierta(incidencia);
        seguimientos.save(new FichaSeguimiento(id, pedido.nota().trim(), limpiarUsuario(usuario)));
        return armar(incidencia);
    }

    /**
     * Deja constancia de lo que se reparo y del costo final. Se llama justo antes de cerrar la incidencia. Si la
     * incidencia ya estaba cerrada (un reintento), no cambia nada.
     */
    @Transactional
    public void registrarCierre(UUID id, CierreRequest cierre, String usuario, String sucursalUsuario) {
        Incidencia incidencia = cargarAccesible(id, sucursalUsuario);
        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            return;
        }
        String quien = limpiarUsuario(usuario);
        FichaTaller ficha = fichas.findById(id).orElseGet(() -> new FichaTaller(id, quien));
        ficha.registrarCierre(cierre.trabajoRealizado().trim(), cierre.costoFinal(), quien);
        fichas.save(ficha);
    }

    // ---------------------------------------------------------------- para el listado de incidencias

    @Transactional(readOnly = true)
    public PaginaIncidenciasResponse enriquecer(PaginaIncidenciasResponse pagina) {
        if (pagina.incidencias().isEmpty()) {
            return pagina;
        }
        Map<UUID, FichaTaller> porId = fichas.findAllById(
                        pagina.incidencias().stream().map(IncidenciaResponse::idIncidencia).toList())
                .stream().collect(Collectors.toMap(FichaTaller::getIdIncidencia, Function.identity()));
        List<IncidenciaResponse> lista = pagina.incidencias().stream()
                .map(r -> porId.containsKey(r.idIncidencia())
                        ? r.conFicha(porId.get(r.idIncidencia()).getFechaSalidaPrevista())
                        : r)
                .toList();
        return new PaginaIncidenciasResponse(pagina.total(), pagina.pagina(), pagina.tamano(), pagina.totalPaginas(), lista);
    }

    @Transactional(readOnly = true)
    public IncidenciaResponse enriquecer(IncidenciaResponse respuesta) {
        return fichas.findById(respuesta.idIncidencia())
                .map(f -> respuesta.conFicha(f.getFechaSalidaPrevista()))
                .orElse(respuesta);
    }

    // ---------------------------------------------------------------- internos

    private FichaResponse armar(Incidencia incidencia) {
        UUID id = incidencia.getId();
        return FichaResponse.de(id, incidencia.getEstado().name(), ingreso(incidencia), fichas.findById(id).orElse(null),
                internos.findByIdIncidenciaOrderByOrden(id), externos.findByIdIncidenciaOrderByOrden(id),
                seguimientos.findByIdIncidenciaOrderByRegistradoEnDesc(id));
    }

    private Incidencia cargarAccesible(UUID id, String sucursalUsuario) {
        Incidencia incidencia = incidencias.findById(id).orElseThrow(IncidenciaException::incidenciaNoEncontrada);
        if (sucursalUsuario != null && !sucursalUsuario.isBlank()
                && !incidencia.getSucursal().equalsIgnoreCase(sucursalUsuario.trim())) {
            throw IncidenciaException.incidenciaNoEncontrada();   // fuera de su sede: como si no existiera
        }
        return incidencia;
    }

    private static void exigirAbierta(Incidencia incidencia) {
        if (incidencia.getEstado() == EstadoIncidencia.CERRADA) {
            throw IncidenciaException.incidenciaCerrada();
        }
    }

    private static LocalDate ingreso(Incidencia incidencia) {
        return incidencia.getCreadoEn().atZone(ZONA_LIMA).toLocalDate();
    }

    private static String limpiarUsuario(String usuario) {
        String limpio = usuario == null || usuario.isBlank() ? "desconocido" : usuario.trim();
        return limpio.length() > 120 ? limpio.substring(0, 120) : limpio;
    }

    private static String limpiarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
