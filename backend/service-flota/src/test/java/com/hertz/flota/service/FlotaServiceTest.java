package com.hertz.flota.service;

import com.hertz.flota.dto.BloqueoRequest;
import com.hertz.flota.dto.CrearVehiculoRequest;
import com.hertz.flota.entity.BloqueoVehiculo;
import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;
import com.hertz.flota.exception.FlotaException;
import com.hertz.flota.entity.HistorialVehiculo;
import com.hertz.flota.repository.BloqueoRepository;
import com.hertz.flota.repository.HistorialRepository;
import com.hertz.flota.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de FlotaService. No usan la base de datos: el repositorio
 * se reemplaza por un mock (Mockito), así que corren en milisegundos y sin conexión.
 */
class FlotaServiceTest {

    private static final String USUARIO = "mauro@hertz.pe";
    // Sucursal de los vehículos de prueba (ver vehiculo()).
    private static final String SEDE = "LIMA_AEROPUERTO";

    private VehiculoRepository repo;
    private HistorialRepository historial;
    private BloqueoRepository bloqueos;
    private FlotaService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(VehiculoRepository.class);
        historial = Mockito.mock(HistorialRepository.class);
        bloqueos = Mockito.mock(BloqueoRepository.class);
        service = new FlotaService(repo, historial, bloqueos);
        when(bloqueos.save(any(BloqueoVehiculo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repo.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Vehiculo vehiculo(String placa, EstadoVehiculo estado) {
        try {
            var ctor = Vehiculo.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Vehiculo v = ctor.newInstance();
            ReflectionTestUtils.setField(v, "placa", placa);
            ReflectionTestUtils.setField(v, "categoria", "SUV");
            ReflectionTestUtils.setField(v, "marca", "Toyota");
            ReflectionTestUtils.setField(v, "modelo", "RAV4");
            ReflectionTestUtils.setField(v, "anio", (short) 2024);
            ReflectionTestUtils.setField(v, "sucursal", "LIMA_AEROPUERTO");
            ReflectionTestUtils.setField(v, "estado", estado);
            return v;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void disponibleSePuedeAlquilar() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        var r = service.actualizarEstado("abc-103", EstadoVehiculo.ALQUILADO, "AGENTE", USUARIO, SEDE, null);
        assertEquals(EstadoVehiculo.ALQUILADO, r.estado());
    }

    @Test
    void alquiladoNoSePuedeAlquilarDeNuevo() {
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.ALQUILADO)));
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-104", EstadoVehiculo.ALQUILADO, "AGENTE", USUARIO, SEDE, null));
        assertEquals("VEHICULO_NO_DISPONIBLE", ex.getCodigo());
    }

    @Test
    void enMantenimientoNoSePuedeAlquilar() {
        when(repo.findById("LMA-203")).thenReturn(Optional.of(vehiculo("LMA-203", EstadoVehiculo.MANTENIMIENTO)));
        assertThrows(FlotaException.class,
                () -> service.actualizarEstado("LMA-203", EstadoVehiculo.ALQUILADO, "ADMINISTRADOR", USUARIO, SEDE, null));
    }

    @Test
    void alquiladoPuedeVolverADisponible() {
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.ALQUILADO)));
        var r = service.actualizarEstado("ABC-104", EstadoVehiculo.DISPONIBLE, "AGENTE", USUARIO, SEDE, null);
        assertEquals(EstadoVehiculo.DISPONIBLE, r.estado());
    }

    @Test
    void placaInexistenteDa404() {
        when(repo.findById("XXX-000")).thenReturn(Optional.empty());
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("XXX-000", EstadoVehiculo.DISPONIBLE, "AGENTE", USUARIO, SEDE, null));
        assertEquals("VEHICULO_NO_ENCONTRADO", ex.getCodigo());
    }

    @Test
    void clienteWebNoPuedeCambiarEstado() {
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-103", EstadoVehiculo.ALQUILADO, "CLIENTE", USUARIO, SEDE, null));
        assertEquals("ROL_NO_AUTORIZADO", ex.getCodigo());
    }

    @Test
    void sinRolNoPuedeCambiarEstado() {
        assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-103", EstadoVehiculo.ALQUILADO, null, USUARIO, SEDE, null));
    }

    @Test
    void fechaFinAnteriorAInicioEsInvalida() {
        var ex = assertThrows(FlotaException.class, () -> service.consultarDisponibilidad(
                "LIMA_AEROPUERTO", "SUV", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1)));
        assertEquals("SOLICITUD_INVALIDA", ex.getCodigo());
    }

    @Test
    void cadaCambioDeEstadoQuedaEnElHistorial() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        service.actualizarEstado("ABC-103", EstadoVehiculo.MANTENIMIENTO, "agente", USUARIO, SEDE, "  Golpe en puerta  ");

        ArgumentCaptor<HistorialVehiculo> captor = ArgumentCaptor.forClass(HistorialVehiculo.class);
        verify(historial).save(captor.capture());
        HistorialVehiculo h = captor.getValue();
        assertEquals("ABC-103", h.getPlaca());
        assertEquals(EstadoVehiculo.DISPONIBLE, h.getEstadoAnterior());
        assertEquals(EstadoVehiculo.MANTENIMIENTO, h.getEstadoNuevo());
        assertEquals(USUARIO, h.getUsuario());
        assertEquals("AGENTE", h.getRol());
        assertEquals("Golpe en puerta", h.getMotivo());
    }

    @Test
    void sinUsuarioSeRegistraComoDesconocido() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        service.actualizarEstado("ABC-103", EstadoVehiculo.MANTENIMIENTO, "AGENTE", null, SEDE, "  ");

        ArgumentCaptor<HistorialVehiculo> captor = ArgumentCaptor.forClass(HistorialVehiculo.class);
        verify(historial).save(captor.capture());
        assertEquals("DESCONOCIDO", captor.getValue().getUsuario());
        assertEquals(null, captor.getValue().getMotivo());
    }

    @Test
    void cambioRechazadoNoDejaHistorial() {
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.ALQUILADO)));
        assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-104", EstadoVehiculo.ALQUILADO, "AGENTE", USUARIO, SEDE, null));
        assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-104", EstadoVehiculo.DISPONIBLE, "CLIENTE", USUARIO, SEDE, null));
        verify(historial, never()).save(any());
    }

    @Test
    void historialDeUnVehiculoInexistenteDa404() {
        when(repo.findById("XXX-000")).thenReturn(Optional.empty());
        var ex = assertThrows(FlotaException.class, () -> service.consultarHistorial("XXX-000"));
        assertEquals("VEHICULO_NO_ENCONTRADO", ex.getCodigo());
    }

    @Test
    void listarSinFiltrosBuscaDisponiblesDeTodasLasSucursales() {
        when(repo.listar(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE))));
        var pagina = service.listar(null, null, " ", null, null, null);
        assertEquals(1, pagina.vehiculos().size());
        assertEquals(1, pagina.total());
        assertEquals(0, pagina.pagina());
        assertEquals(12, pagina.tamano());
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", "", PageRequest.of(0, 12));
    }

    @Test
    void listarNormalizaSucursalYTexto() {
        when(repo.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        service.listar(EstadoVehiculo.ALQUILADO, " lima_aeropuerto ", " Toyota ", null, 0, 12);
        verify(repo).listar(EstadoVehiculo.ALQUILADO, "LIMA_AEROPUERTO", "%toyota%", "", PageRequest.of(0, 12));
    }

    @Test
    void listarLimitaElTamanoDePaginaYNoAceptaPaginasNegativas() {
        when(repo.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        service.listar(null, null, null, null, -5, 9999);
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", "", PageRequest.of(0, 50));
        service.listar(null, null, null, null, 2, 0);
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", "", PageRequest.of(2, 1));
    }

    @Test
    void listarInformaElTotalYLasPaginas() {
        var vehiculos = List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE));
        when(repo.listar(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(vehiculos, PageRequest.of(1, 1), 5));
        var pagina = service.listar(null, null, null, null, 1, 1);
        assertEquals(5, pagina.total());
        assertEquals(5, pagina.totalPaginas());
        assertEquals(1, pagina.pagina());
    }
    @Test
    void agenteNoPuedeCambiarVehiculoDeOtraSucursal() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        var ex = assertThrows(FlotaException.class, () -> service.actualizarEstado(
                "ABC-103", EstadoVehiculo.MANTENIMIENTO, "AGENTE", USUARIO, "CUSCO_AEROPUERTO", null));
        assertEquals("SUCURSAL_NO_AUTORIZADA", ex.getCodigo());
        verify(historial, never()).save(any());
    }

    @Test
    void agenteSinSucursalNoPuedeCambiarEstado() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        var ex = assertThrows(FlotaException.class, () -> service.actualizarEstado(
                "ABC-103", EstadoVehiculo.MANTENIMIENTO, "AGENTE", USUARIO, null, null));
        assertEquals("SUCURSAL_NO_AUTORIZADA", ex.getCodigo());
    }

    @Test
    void administradorDeSucursalCambiaSoloSuSucursal() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        var ok = service.actualizarEstado("ABC-103", EstadoVehiculo.MANTENIMIENTO,
                "ADMINISTRADOR_SUCURSAL", USUARIO, "lima_aeropuerto", null);
        assertEquals(EstadoVehiculo.MANTENIMIENTO, ok.estado());

        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.DISPONIBLE)));
        assertThrows(FlotaException.class, () -> service.actualizarEstado("ABC-104",
                EstadoVehiculo.MANTENIMIENTO, "ADMINISTRADOR_SUCURSAL", USUARIO, "LIMA_MIRAFLORES", null));
    }

    @Test
    void administradorGeneralCambiaCualquierSucursalAunSinSucursal() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        var r = service.actualizarEstado("ABC-103", EstadoVehiculo.MANTENIMIENTO, "ADMINISTRADOR", USUARIO, null, null);
        assertEquals(EstadoVehiculo.MANTENIMIENTO, r.estado());
    }

    // ---------- Apartado de vehiculos (ServicioReserva) ----------

    private static final LocalDate INI = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 5);

    private BloqueoRequest pedido(String referencia) {
        return new BloqueoRequest(SEDE, "suv", INI, FIN, referencia, null);
    }

    @Test
    void apartarCreaUnBloqueoConLaReferenciaDeLaReserva() {
        when(bloqueos.findFirstByReferenciaAndMotivo("R-1", "RESERVA")).thenReturn(Optional.empty());
        when(repo.tomarDisponibles(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));

        var r = service.apartar(pedido("R-1"), "SERVICIO");

        assertEquals("ABC-103", r.placa());
        assertEquals("R-1", r.referencia());
        ArgumentCaptor<BloqueoVehiculo> captor = ArgumentCaptor.forClass(BloqueoVehiculo.class);
        verify(bloqueos).save(captor.capture());
        assertEquals("RESERVA", captor.getValue().getMotivo());
        assertEquals(INI, captor.getValue().getFechaInicio());
        verify(repo).tomarDisponibles(SEDE, "SUV", EstadoVehiculo.DISPONIBLE, INI, FIN, PageRequest.of(0, 1));
    }

    @Test
    void apartarSinVehiculosLibresResponde409SinDisponibilidad() {
        when(bloqueos.findFirstByReferenciaAndMotivo(any(), any())).thenReturn(Optional.empty());
        when(repo.tomarDisponibles(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());

        var ex = assertThrows(FlotaException.class, () -> service.apartar(pedido("R-2"), "SERVICIO"));
        assertEquals("SIN_DISPONIBILIDAD", ex.getCodigo());
        verify(bloqueos, never()).save(any());
    }

    @Test
    void apartarEsIdempotenteSiLaReferenciaYaTieneVehiculo() {
        var existente = new BloqueoVehiculo("ABC-103", INI, FIN, "RESERVA", "R-3");
        when(bloqueos.findFirstByReferenciaAndMotivo("R-3", "RESERVA")).thenReturn(Optional.of(existente));
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));

        var r = service.apartar(pedido("R-3"), "SERVICIO");

        assertEquals("ABC-103", r.placa());
        verify(repo, never()).tomarDisponibles(any(), any(), any(), any(), any(), any());
        verify(bloqueos, never()).save(any());
    }

    @Test
    void soloElRolServicioPuedeApartarOLiberar() {
        for (String rol : new String[]{"CLIENTE", "AGENTE", "ADMINISTRADOR", null}) {
            var ex = assertThrows(FlotaException.class, () -> service.apartar(pedido("R-4"), rol));
            assertEquals("ROL_NO_AUTORIZADO", ex.getCodigo());
            assertThrows(FlotaException.class, () -> service.liberar("R-4", rol));
        }
    }

    @Test
    void apartarRechazaFechasInvertidas() {
        var invertido = new BloqueoRequest(SEDE, "SUV", FIN, INI, "R-5", null);
        var ex = assertThrows(FlotaException.class, () -> service.apartar(invertido, "SERVICIO"));
        assertEquals("SOLICITUD_INVALIDA", ex.getCodigo());
    }

    @Test
    void reprogramarLiberaElBloqueoViejoYApartaUnoNuevo() {
        var viejo = new BloqueoVehiculo("ABC-103", INI, FIN, "RESERVA", "R-6");
        when(bloqueos.findFirstByReferenciaAndMotivo("R-6", "RESERVA")).thenReturn(Optional.of(viejo));
        when(repo.tomarDisponibles(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));

        var nuevo = new BloqueoRequest(SEDE, "SUV", INI.plusDays(10), FIN.plusDays(10), "otra", null);
        var r = service.reprogramar("R-6", nuevo, "SERVICIO");

        verify(bloqueos).delete(viejo);
        assertEquals("R-6", r.referencia());          // conserva la referencia de la reserva
        assertEquals(INI.plusDays(10), r.fechaInicio());
    }

    @Test
    void reprogramarSinCupoLanzaErrorParaQueSeDeshagaLaTransaccion() {
        var viejo = new BloqueoVehiculo("ABC-103", INI, FIN, "RESERVA", "R-7");
        when(bloqueos.findFirstByReferenciaAndMotivo("R-7", "RESERVA")).thenReturn(Optional.of(viejo));
        when(repo.tomarDisponibles(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());

        var ex = assertThrows(FlotaException.class, () -> service.reprogramar("R-7", pedido("R-7"), "SERVICIO"));
        assertEquals("SIN_DISPONIBILIDAD", ex.getCodigo());   // @Transactional revierte el delete
    }

    @Test
    void reprogramarUnaReferenciaInexistenteResponde404() {
        when(bloqueos.findFirstByReferenciaAndMotivo(any(), any())).thenReturn(Optional.empty());
        var ex = assertThrows(FlotaException.class, () -> service.reprogramar("R-8", pedido("R-8"), "SERVICIO"));
        assertEquals("BLOQUEO_NO_ENCONTRADO", ex.getCodigo());
    }

    @Test
    void liberarBorraElBloqueoYNoFallaSiYaNoExiste() {
        var b = new BloqueoVehiculo("ABC-103", INI, FIN, "RESERVA", "R-9");
        when(bloqueos.findFirstByReferenciaAndMotivo("R-9", "RESERVA")).thenReturn(Optional.of(b));
        service.liberar("R-9", "SERVICIO");
        verify(bloqueos, times(1)).delete(b);

        when(bloqueos.findFirstByReferenciaAndMotivo("R-10", "RESERVA")).thenReturn(Optional.empty());
        service.liberar("R-10", "SERVICIO");   // no lanza
        assertTrue(true);
    }

    @Test
    void listarPoneEnPrimerLugarLaSucursalPrioritariaNormalizada() {
        when(repo.listar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        service.listar(null, null, null, " lima_miraflores ", 0, 12);
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", "LIMA_MIRAFLORES", PageRequest.of(0, 12));
    }

    // ---------- Alta de vehiculos (solo ADMINISTRADOR general) ----------

    private static CrearVehiculoRequest nuevo(String placa) {
        return new CrearVehiculoRequest(placa, "suv", "Toyota", "Hilux", 2025, "lima_aeropuerto");
    }

    @Test
    void elAdministradorGeneralRegistraUnVehiculoNuevoDisponibleYQuedaEnElHistorial() {
        when(repo.existsById("XYZ-123")).thenReturn(false);

        var r = service.crearVehiculo(nuevo(" xyz-123 "), "ADMINISTRADOR", "admin@hertz.test");

        assertEquals("XYZ-123", r.placa());
        assertEquals(EstadoVehiculo.DISPONIBLE, r.estado());
        assertEquals("SUV", r.categoria());
        assertEquals("LIMA_AEROPUERTO", r.sucursal());
        ArgumentCaptor<HistorialVehiculo> h = ArgumentCaptor.forClass(HistorialVehiculo.class);
        verify(historial).save(h.capture());
        assertEquals(null, h.getValue().getEstadoAnterior());   // el alta no tiene estado anterior
        assertEquals(EstadoVehiculo.DISPONIBLE, h.getValue().getEstadoNuevo());
        assertEquals("admin@hertz.test", h.getValue().getUsuario());
        assertEquals("ADMINISTRADOR", h.getValue().getRol());
    }

    @Test
    void soloElAdministradorGeneralPuedeRegistrarVehiculos() {
        for (String rol : new String[]{"AGENTE", "ADMINISTRADOR_SUCURSAL", "CLIENTE", null}) {
            var ex = assertThrows(FlotaException.class, () -> service.crearVehiculo(nuevo("XYZ-123"), rol, "x"));
            assertEquals("ROL_NO_AUTORIZADO", ex.getCodigo());
        }
        verify(repo, never()).save(any());
    }

    @Test
    void noSeRegistraUnaPlacaQueYaExiste() {
        when(repo.existsById("XYZ-123")).thenReturn(true);
        var ex = assertThrows(FlotaException.class, () -> service.crearVehiculo(nuevo("XYZ-123"), "ADMINISTRADOR", "a"));
        assertEquals("PLACA_DUPLICADA", ex.getCodigo());
        verify(repo, never()).save(any());
    }

    @Test
    void elAltaValidaPlacaCategoriaSucursalYAnio() {
        var malaPlaca = new CrearVehiculoRequest("123", "SUV", "Kia", "Rio", 2024, "LIMA_AEROPUERTO");
        var malaCategoria = new CrearVehiculoRequest("XYZ-123", "LIMUSINA", "Kia", "Rio", 2024, "LIMA_AEROPUERTO");
        var malaSucursal = new CrearVehiculoRequest("XYZ-123", "SUV", "Kia", "Rio", 2024, "MARTE");
        var anioViejo = new CrearVehiculoRequest("XYZ-123", "SUV", "Kia", "Rio", 1990, "LIMA_AEROPUERTO");
        var anioFuturo = new CrearVehiculoRequest("XYZ-123", "SUV", "Kia", "Rio", 3000, "LIMA_AEROPUERTO");
        for (var pedido : new CrearVehiculoRequest[]{malaPlaca, malaCategoria, malaSucursal, anioViejo, anioFuturo}) {
            var ex = assertThrows(FlotaException.class, () -> service.crearVehiculo(pedido, "ADMINISTRADOR", "a"));
            assertEquals("SOLICITUD_INVALIDA", ex.getCodigo());
        }
        verify(repo, never()).save(any());
    }

    // ---------- Baja de vehiculos (FUERA_DE_SERVICIO, solo ADMINISTRADOR general) ----------

    @Test
    void elAdministradorGeneralDaDeBajaUnVehiculoLibre() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        var r = service.actualizarEstado("ABC-103", EstadoVehiculo.FUERA_DE_SERVICIO, "ADMINISTRADOR", USUARIO, null, "Venta del vehiculo");
        assertEquals(EstadoVehiculo.FUERA_DE_SERVICIO, r.estado());
    }

    @Test
    void nadieMasPuedeDarDeBajaNiReactivar() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        for (String rol : new String[]{"AGENTE", "ADMINISTRADOR_SUCURSAL"}) {
            var ex = assertThrows(FlotaException.class,
                    () -> service.actualizarEstado("ABC-103", EstadoVehiculo.FUERA_DE_SERVICIO, rol, USUARIO, SEDE, null));
            assertEquals("ROL_NO_AUTORIZADO", ex.getCodigo());
        }
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.FUERA_DE_SERVICIO)));
        var reactivar = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-104", EstadoVehiculo.DISPONIBLE, "AGENTE", USUARIO, SEDE, null));
        assertEquals("ROL_NO_AUTORIZADO", reactivar.getCodigo());
    }

    @Test
    void noSeDaDeBajaUnVehiculoAlquilado() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.ALQUILADO)));
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-103", EstadoVehiculo.FUERA_DE_SERVICIO, "ADMINISTRADOR", USUARIO, null, null));
        assertEquals("VEHICULO_ALQUILADO", ex.getCodigo());
    }

    @Test
    void noSeDaDeBajaUnVehiculoConReservasPendientes() {
        when(repo.findById("ABC-103")).thenReturn(Optional.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));
        when(bloqueos.countByPlacaAndFechaFinGreaterThanEqualAndMotivoIn(any(), any(), any())).thenReturn(2L);
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-103", EstadoVehiculo.FUERA_DE_SERVICIO, "ADMINISTRADOR", USUARIO, null, null));
        assertEquals("VEHICULO_CON_RESERVAS", ex.getCodigo());
        assertTrue(ex.getMessage().contains("2 reservas"));
    }

    @Test
    void unVehiculoDadoDeBajaSoloSePuedeReactivar() {
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.FUERA_DE_SERVICIO)));
        for (EstadoVehiculo destino : new EstadoVehiculo[]{EstadoVehiculo.ALQUILADO, EstadoVehiculo.MANTENIMIENTO}) {
            var ex = assertThrows(FlotaException.class,
                    () -> service.actualizarEstado("ABC-104", destino, "ADMINISTRADOR", USUARIO, null, null));
            assertEquals("TRANSICION_INVALIDA", ex.getCodigo());
        }
        var r = service.actualizarEstado("ABC-104", EstadoVehiculo.DISPONIBLE, "ADMINISTRADOR", USUARIO, null, "Reactivacion tras revision");
        assertEquals(EstadoVehiculo.DISPONIBLE, r.estado());
    }

    // ---------- Vehiculo elegido por el cliente ----------

    @Test
    void apartarConPlacaElegidaApartaEsaPlaca() {
        when(bloqueos.findFirstByReferenciaAndMotivo("R-20", "RESERVA")).thenReturn(Optional.empty());
        when(repo.tomarDisponiblePorPlaca(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));

        var r = service.apartar(new BloqueoRequest(SEDE, "suv", INI, FIN, "R-20", " abc-103 "), "SERVICIO");

        assertEquals("ABC-103", r.placa());
        verify(repo).tomarDisponiblePorPlaca("ABC-103", SEDE, "SUV", EstadoVehiculo.DISPONIBLE, INI, FIN);
        verify(repo, never()).tomarDisponibles(any(), any(), any(), any(), any(), any());
    }

    @Test
    void siElVehiculoElegidoYaNoEstaLibreNoSeApartaOtroEnSuLugar() {
        when(bloqueos.findFirstByReferenciaAndMotivo(any(), any())).thenReturn(Optional.empty());
        when(repo.tomarDisponiblePorPlaca(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        var ex = assertThrows(FlotaException.class,
                () -> service.apartar(new BloqueoRequest(SEDE, "SUV", INI, FIN, "R-21", "ABC-103"), "SERVICIO"));

        assertEquals("VEHICULO_NO_DISPONIBLE", ex.getCodigo());
        verify(repo, never()).tomarDisponibles(any(), any(), any(), any(), any(), any());   // no cae a "cualquiera"
        verify(bloqueos, never()).save(any());
    }

    @Test
    void alReprogramarSeConservaElMismoVehiculoSiSigueLibre() {
        var viejo = new BloqueoVehiculo("ABC-103", INI, FIN, "RESERVA", "R-22");
        when(bloqueos.findFirstByReferenciaAndMotivo("R-22", "RESERVA")).thenReturn(Optional.of(viejo));
        when(repo.tomarDisponiblePorPlaca(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE)));

        var r = service.reprogramar("R-22", pedido("R-22"), "SERVICIO");

        assertEquals("ABC-103", r.placa());
        verify(repo, never()).tomarDisponibles(any(), any(), any(), any(), any(), any());
    }

    @Test
    void alReprogramarSiElMismoVehiculoYaNoEstaLibreSeBuscaOtroDeLaCategoria() {
        var viejo = new BloqueoVehiculo("ABC-103", INI, FIN, "RESERVA", "R-23");
        when(bloqueos.findFirstByReferenciaAndMotivo("R-23", "RESERVA")).thenReturn(Optional.of(viejo));
        when(repo.tomarDisponiblePorPlaca(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(repo.tomarDisponibles(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(vehiculo("ABC-104", EstadoVehiculo.DISPONIBLE)));

        assertEquals("ABC-104", service.reprogramar("R-23", pedido("R-23"), "SERVICIO").placa());
    }
}
