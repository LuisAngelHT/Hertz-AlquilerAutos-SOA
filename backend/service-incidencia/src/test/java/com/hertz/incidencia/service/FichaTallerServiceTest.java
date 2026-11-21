package com.hertz.incidencia.service;

import com.hertz.incidencia.dto.CierreRequest;
import com.hertz.incidencia.dto.DanoExternoRequest;
import com.hertz.incidencia.dto.DanoInternoRequest;
import com.hertz.incidencia.dto.FichaRequest;
import com.hertz.incidencia.dto.IncidenciaResponse;
import com.hertz.incidencia.dto.PaginaIncidenciasResponse;
import com.hertz.incidencia.dto.SeguimientoRequest;
import com.hertz.incidencia.entity.FichaDanoExterno;
import com.hertz.incidencia.entity.FichaDanoInterno;
import com.hertz.incidencia.entity.FichaSeguimiento;
import com.hertz.incidencia.entity.FichaTaller;
import com.hertz.incidencia.entity.GravedadDano;
import com.hertz.incidencia.entity.Incidencia;
import com.hertz.incidencia.entity.SistemaVehiculo;
import com.hertz.incidencia.entity.TipoDano;
import com.hertz.incidencia.entity.TipoIncidencia;
import com.hertz.incidencia.entity.VistaVehiculo;
import com.hertz.incidencia.exception.IncidenciaException;
import com.hertz.incidencia.repository.FichaDanoExternoRepository;
import com.hertz.incidencia.repository.FichaDanoInternoRepository;
import com.hertz.incidencia.repository.FichaSeguimientoRepository;
import com.hertz.incidencia.repository.FichaTallerRepository;
import com.hertz.incidencia.repository.IncidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FichaTallerServiceTest {

    private static final String LIMA = "LIMA_AEROPUERTO";
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private IncidenciaRepository incidencias;
    private FichaTallerRepository fichas;
    private FichaDanoInternoRepository internos;
    private FichaDanoExternoRepository externos;
    private FichaSeguimientoRepository seguimientos;
    private FichaTallerService service;

    @BeforeEach
    void setUp() {
        incidencias = Mockito.mock(IncidenciaRepository.class);
        fichas = Mockito.mock(FichaTallerRepository.class);
        internos = Mockito.mock(FichaDanoInternoRepository.class);
        externos = Mockito.mock(FichaDanoExternoRepository.class);
        seguimientos = Mockito.mock(FichaSeguimientoRepository.class);
        service = new FichaTallerService(incidencias, fichas, internos, externos, seguimientos);
    }

    private Incidencia abierta() {
        Incidencia i = new Incidencia(UUID.randomUUID(), "ABC-104", LIMA, UUID.randomUUID(), TipoIncidencia.SINIESTRO,
                "Choque", new BigDecimal("500"), "DISPONIBLE", "agente@hertz.test");
        when(incidencias.findById(i.getId())).thenReturn(Optional.of(i));
        return i;
    }

    private static FichaRequest ficha(Integer dias, DanoInternoRequest[] internos, DanoExternoRequest[] externos) {
        return new FichaRequest("Radiador roto y motor con sobrecalentamiento", dias, new BigDecimal("800.00"),
                new BigDecimal("350.00"), List.of(internos), List.of(externos));
    }

    private static DanoInternoRequest interno(String descripcion) {
        return new DanoInternoRequest(SistemaVehiculo.ENFRIAMIENTO, descripcion, GravedadDano.GRAVE, new BigDecimal("600"));
    }

    private static DanoExternoRequest externo(String descripcion) {
        return new DanoExternoRequest(VistaVehiculo.FRENTE, new BigDecimal("48.50"), new BigDecimal("40.00"),
                TipoDano.ABOLLADURA, descripcion, GravedadDano.MODERADO, new BigDecimal("250"));
    }

    @Test
    void guardaElPlanYCalculaLaFechaDeSalidaDesdeElIngresoATaller() {
        Incidencia i = abierta();

        service.guardar(i.getId(), ficha(4, new DanoInternoRequest[] {interno("Radiador roto")},
                new DanoExternoRequest[] {externo("Capó hundido")}), "mecanico@hertz.test", LIMA);

        ArgumentCaptor<FichaTaller> guardada = ArgumentCaptor.forClass(FichaTaller.class);
        verify(fichas).save(guardada.capture());
        LocalDate ingreso = i.getCreadoEn().atZone(ZONA).toLocalDate();
        assertEquals(4, guardada.getValue().getDiasEstimados());
        assertEquals(ingreso.plusDays(4), guardada.getValue().getFechaSalidaPrevista());   // ingreso + 4 dias
        assertEquals(new BigDecimal("800.00"), guardada.getValue().getCostoRepuestos());
        assertEquals("mecanico@hertz.test", guardada.getValue().getActualizadoPor());
    }

    @Test
    void sinDiasEstimadosNoHayFechaDeSalida() {
        Incidencia i = abierta();
        service.guardar(i.getId(), ficha(null, new DanoInternoRequest[0], new DanoExternoRequest[0]), "m@hertz.test", null);
        ArgumentCaptor<FichaTaller> guardada = ArgumentCaptor.forClass(FichaTaller.class);
        verify(fichas).save(guardada.capture());
        assertNull(guardada.getValue().getFechaSalidaPrevista());
    }

    @Test
    void lasListasDeDanosSeReescribenCompletasYEnOrden() {
        Incidencia i = abierta();

        service.guardar(i.getId(), ficha(2,
                new DanoInternoRequest[] {interno("Primero"), interno("Segundo")},
                new DanoExternoRequest[] {externo("Externo")}), "m@hertz.test", LIMA);

        var orden = Mockito.inOrder(internos, externos);
        orden.verify(internos).deleteByIdIncidencia(i.getId());   // primero se borra lo anterior...
        ArgumentCaptor<FichaDanoInterno> in = ArgumentCaptor.forClass(FichaDanoInterno.class);
        verify(internos, Mockito.times(2)).save(in.capture());
        assertEquals(0, in.getAllValues().get(0).getOrden());     // ...y se guarda en el orden recibido
        assertEquals("Segundo", in.getAllValues().get(1).getDescripcion());
        assertEquals(1, in.getAllValues().get(1).getOrden());
        ArgumentCaptor<FichaDanoExterno> ex = ArgumentCaptor.forClass(FichaDanoExterno.class);
        verify(externos).save(ex.capture());
        assertEquals(VistaVehiculo.FRENTE, ex.getValue().getVista());
        assertEquals(new BigDecimal("48.50"), ex.getValue().getPosX());
    }

    @Test
    void unaIncidenciaCerradaSoloSeConsulta() {
        Incidencia i = abierta();
        i.cerrar();

        assertEquals("INCIDENCIA_CERRADA", assertThrows(IncidenciaException.class, () -> service.guardar(i.getId(),
                ficha(1, new DanoInternoRequest[0], new DanoExternoRequest[0]), "m@hertz.test", LIMA)).getCodigo());
        assertEquals("INCIDENCIA_CERRADA", assertThrows(IncidenciaException.class, () -> service.agregarSeguimiento(
                i.getId(), new SeguimientoRequest("nota"), "m@hertz.test", LIMA)).getCodigo());
        verify(fichas, never()).save(any());
        assertEquals("CERRADA", service.obtener(i.getId(), LIMA).estadoIncidencia());   // consultar si se puede
    }

    @Test
    void unaSucursalNoVeNiTocaLasFichasDeOtra() {
        Incidencia i = abierta();   // sucursal LIMA_AEROPUERTO
        assertEquals("INCIDENCIA_NO_ENCONTRADA", assertThrows(IncidenciaException.class,
                () -> service.obtener(i.getId(), "CUSCO_AEROPUERTO")).getCodigo());
        assertEquals("INCIDENCIA_NO_ENCONTRADA", assertThrows(IncidenciaException.class, () -> service.guardar(i.getId(),
                ficha(1, new DanoInternoRequest[0], new DanoExternoRequest[0]), "m@hertz.test", "CUSCO_AEROPUERTO")).getCodigo());
        service.obtener(i.getId(), "lima_aeropuerto");   // misma sede (sin importar mayusculas) y sin sede (admin): si
        service.obtener(i.getId(), null);
    }

    @Test
    void elSeguimientoRegistraQuienYCuando() {
        Incidencia i = abierta();
        service.agregarSeguimiento(i.getId(), new SeguimientoRequest("  Llegó el radiador nuevo  "), "mecanico@hertz.test", LIMA);

        ArgumentCaptor<FichaSeguimiento> nota = ArgumentCaptor.forClass(FichaSeguimiento.class);
        verify(seguimientos).save(nota.capture());
        assertEquals("Llegó el radiador nuevo", nota.getValue().getNota());
        assertEquals("mecanico@hertz.test", nota.getValue().getRegistradoPor());
        assertTrue(nota.getValue().getRegistradoEn() != null);
    }

    @Test
    void alCerrarQuedaLoQueSeRepararYElCostoFinal() {
        Incidencia i = abierta();
        when(fichas.findById(i.getId())).thenReturn(Optional.empty());

        service.registrarCierre(i.getId(), new CierreRequest("  Cambio de radiador y capó  ", new BigDecimal("1250.00")),
                "mecanico@hertz.test", LIMA);

        ArgumentCaptor<FichaTaller> guardada = ArgumentCaptor.forClass(FichaTaller.class);
        verify(fichas).save(guardada.capture());
        assertEquals("Cambio de radiador y capó", guardada.getValue().getTrabajoRealizado());
        assertEquals(new BigDecimal("1250.00"), guardada.getValue().getCostoFinal());
    }

    @Test
    void repetirElCierreDeUnaIncidenciaYaCerradaNoCambiaNada() {
        Incidencia i = abierta();
        i.cerrar();
        service.registrarCierre(i.getId(), new CierreRequest("x", BigDecimal.ONE), "m@hertz.test", LIMA);
        verify(fichas, never()).save(any());
    }

    @Test
    void elListadoMarcaLasIncidenciasQueYaTienenFichaConSuFechaDeSalida() {
        Incidencia conFicha = abierta();
        Incidencia sinFicha = abierta();
        LocalDate salida = LocalDate.now(ZONA).plusDays(3);
        FichaTaller ficha = new FichaTaller(conFicha.getId(), "m@hertz.test");
        ficha.actualizarPlan("d", 3, salida, null, null, "m@hertz.test");
        when(fichas.findAllById(any())).thenReturn(List.of(ficha));

        var pagina = new PaginaIncidenciasResponse(2, 0, 20, 1, List.of(respuesta(conFicha), respuesta(sinFicha)));
        var resultado = service.enriquecer(pagina);

        assertTrue(resultado.incidencias().get(0).tieneFicha());
        assertEquals(salida, resultado.incidencias().get(0).fechaSalidaPrevista());
        assertFalse(resultado.incidencias().get(1).tieneFicha());
        assertNull(resultado.incidencias().get(1).fechaSalidaPrevista());
        verify(fichas, Mockito.times(1)).findAllById(any());   // una sola consulta para toda la pagina
    }

    private static IncidenciaResponse respuesta(Incidencia i) {
        return new IncidenciaResponse(i.getId(), i.getPlaca(), i.getSucursal(), i.getIdContrato(), i.getTipo(),
                i.getDescripcion(), i.getCostoEstimado(), i.getEstado(), i.getRegistradaPor(), i.getCreadoEn());
    }
}
