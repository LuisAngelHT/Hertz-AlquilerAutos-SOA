package com.hertz.reserva.service;

import com.hertz.reserva.client.ClienteClient;
import com.hertz.reserva.client.ClienteClient.ClienteInfo;
import com.hertz.reserva.client.FlotaClient;
import com.hertz.reserva.client.FlotaClient.Apartado;
import com.hertz.reserva.dto.ModificarReservaRequest;
import com.hertz.reserva.dto.ReservaRequest;
import com.hertz.reserva.entity.EstadoReserva;
import com.hertz.reserva.entity.HistorialReserva;
import com.hertz.reserva.entity.Reserva;
import com.hertz.reserva.exception.ReservaException;
import com.hertz.reserva.repository.HistorialReservaRepository;
import com.hertz.reserva.repository.ReservaRepository;
import com.hertz.reserva.service.ReservaService.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las reglas de ReservaService. Sin base de datos ni red: repositorios y clientes
 * de flota/cliente son mocks, y el reloj es fijo (hoy = 2026-11-18).
 */
class ReservaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 18);
    private static final LocalDate INI = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 5);
    private static final String LIMA = "LIMA_AEROPUERTO";
    private static final UUID CLIENTE = UUID.randomUUID();

    private static final Actor AGENTE_LIMA = new Actor("agente@hertz.test", "AGENTE", LIMA);
    private static final Actor AGENTE_CUSCO = new Actor("cusco@hertz.test", "AGENTE", "CUSCO_AEROPUERTO");
    private static final Actor ADMIN = new Actor("admin@hertz.test", "ADMINISTRADOR", null);
    private static final Actor CLIENTE_ACTOR = new Actor("cliente@hertz.test", "CLIENTE", null);

    private ReservaRepository reservas;
    private HistorialReservaRepository historial;
    private FlotaClient flota;
    private ClienteClient clientes;
    private ReservaService service;

    @BeforeEach
    void setUp() {
        reservas = Mockito.mock(ReservaRepository.class);
        historial = Mockito.mock(HistorialReservaRepository.class);
        flota = Mockito.mock(FlotaClient.class);
        clientes = Mockito.mock(ClienteClient.class);
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        service = new ReservaService(reservas, historial, flota, clientes,
                Mockito.mock(PlatformTransactionManager.class), reloj, 60);

        when(reservas.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientes.obtener(CLIENTE)).thenReturn(info(CLIENTE));
        when(clientes.porCorreo("cliente@hertz.test")).thenReturn(info(CLIENTE));
        when(flota.apartar(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenAnswer(inv -> new Apartado(inv.getArgument(0), "ABC-103"));
        when(flota.reprogramar(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> new Apartado(inv.getArgument(0), "ABC-103"));
    }

    private ReservaRequest pedido() {
        return new ReservaRequest(CLIENTE, "suv", LIMA, LIMA, INI, FIN, null);
    }

    private static ClienteInfo info(UUID id) {
        return new ClienteInfo(id, "DNI", "12345678", "Ana", "Prueba", "ana@mail.com", "999888777",
                "Q12345678", LocalDate.of(2035, 1, 1));
    }

    private Reserva reserva(EstadoReserva estado) {
        return reserva(estado, LIMA);
    }

    private Reserva reserva(EstadoReserva estado, String devolucion) {
        Reserva r = new Reserva(UUID.randomUUID(), CLIENTE, "SUV", LIMA, devolucion, INI, FIN, "ABC-103", "agente@hertz.test",
                "Ana Prueba", "DNI 12345678", "999888777");
        if (estado != EstadoReserva.CONFIRMADA) {
            r.cambiarEstado(estado);
        }
        when(reservas.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    private static ModificarReservaRequest cambioEstado(EstadoReserva e) {
        return new ModificarReservaRequest(null, null, null, e, null);
    }

    private static ReservaException error(Runnable accion) {
        return assertThrows(ReservaException.class, accion::run);
    }

    // ---------------------------------------------------------------- crear

    @Test
    void crearApartaVehiculoYGuardaReservaConfirmadaConHistorial() {
        var r = service.crear(pedido(), AGENTE_LIMA);

        assertEquals(EstadoReserva.CONFIRMADA, r.estado());
        assertEquals("SUV", r.categoria());
        assertEquals("ABC-103", r.placaAsignada());
        assertEquals(CLIENTE, r.idCliente());
        // La referencia en flota es el id de la reserva.
        verify(flota).apartar(r.idReserva().toString(), LIMA, "SUV", INI, FIN, null);
        ArgumentCaptor<HistorialReserva> h = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(h.capture());
        assertEquals("CREADA", h.getValue().getAccion());
        assertEquals("agente@hertz.test", h.getValue().getUsuario());
        assertEquals("AGENTE", h.getValue().getRol());
    }

    @Test
    void crearComoClienteUsaSuPropioPerfilSinImportarElIdEnviado() {
        var otro = new ReservaRequest(UUID.randomUUID(), "SUV", LIMA, LIMA, INI, FIN, null);
        var r = service.crear(otro, CLIENTE_ACTOR);
        assertEquals(CLIENTE, r.idCliente());
        verify(clientes, never()).obtener(any());
    }

    @Test
    void crearSinDisponibilidadNoGuardaNada() {
        when(flota.apartar(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(ReservaException.sinDisponibilidad("sin cupo"));
        var ex = error(() -> service.crear(pedido(), AGENTE_LIMA));
        assertEquals("SIN_DISPONIBILIDAD", ex.getCodigo());
        verify(reservas, never()).save(any());
    }

    @Test
    void crearConClienteInexistenteNoApartaVehiculo() {
        when(clientes.obtener(CLIENTE)).thenThrow(ReservaException.clienteNoEncontrado());
        var ex = error(() -> service.crear(pedido(), AGENTE_LIMA));
        assertEquals("CLIENTE_NO_ENCONTRADO", ex.getCodigo());
        verify(flota, never()).apartar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void siFallaElGuardadoSeLiberaElVehiculoApartado() {
        when(reservas.save(any(Reserva.class))).thenThrow(new IllegalStateException("base caida"));
        assertThrows(IllegalStateException.class, () -> service.crear(pedido(), AGENTE_LIMA));
        verify(flota).liberar(any());
    }

    @Test
    void crearValidaLosDatos() {
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.crear(
                new ReservaRequest(CLIENTE, "limusina", LIMA, LIMA, INI, FIN, null), AGENTE_LIMA)).getCodigo());
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.crear(
                new ReservaRequest(CLIENTE, "SUV", "MARTE", LIMA, INI, FIN, null), AGENTE_LIMA)).getCodigo());
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.crear(
                new ReservaRequest(CLIENTE, "SUV", LIMA, LIMA, FIN, INI, null), AGENTE_LIMA)).getCodigo());       // fin < inicio
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.crear(
                new ReservaRequest(CLIENTE, "SUV", LIMA, LIMA, HOY.minusDays(1), FIN, null), AGENTE_LIMA)).getCodigo());  // pasado
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.crear(
                new ReservaRequest(CLIENTE, "SUV", LIMA, LIMA, INI, INI.plusDays(60), null), AGENTE_LIMA)).getCodigo()); // 61 dias
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.crear(
                new ReservaRequest(null, "SUV", LIMA, LIMA, INI, FIN, null), AGENTE_LIMA)).getCodigo());          // falta idCliente
        verify(flota, never()).apartar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void crearPuedeEmpezarHoyYDurarExactamenteElMaximo() {
        service.crear(new ReservaRequest(CLIENTE, "SUV", LIMA, LIMA, HOY, HOY.plusDays(59), null), AGENTE_LIMA);
        verify(reservas).save(any());
    }

    @Test
    void crearRechazaRolesSinPermiso() {
        assertEquals("ROL_NO_AUTORIZADO", error(() -> service.crear(pedido(), new Actor("x", "OTRO", null))).getCodigo());
        assertEquals("ROL_NO_AUTORIZADO", error(() -> service.crear(pedido(), new Actor("x", null, null))).getCodigo());
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    void cancelarMarcaCanceladaLiberaElVehiculoYRegistraElHistorial() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        var resp = service.cancelar(r.getId(), "cambio de planes", AGENTE_LIMA);

        assertEquals(EstadoReserva.CANCELADA, resp.estado());
        verify(flota).liberar(r.getId().toString());
        ArgumentCaptor<HistorialReserva> h = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(h.capture());
        assertEquals("CANCELADA", h.getValue().getAccion());
        assertEquals(EstadoReserva.CONFIRMADA, h.getValue().getEstadoAnterior());
        assertEquals("cambio de planes", h.getValue().getDetalle());
    }

    @Test
    void noSePuedeCancelarUnaReservaEnCurso() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        assertEquals("TRANSICION_INVALIDA", error(() -> service.cancelar(r.getId(), null, ADMIN)).getCodigo());
        verify(flota, never()).liberar(any());
    }

    @Test
    void cancelarUnaYaCanceladaSoloReintentaLaLiberacion() {
        Reserva r = reserva(EstadoReserva.CANCELADA);
        var resp = service.cancelar(r.getId(), null, ADMIN);
        assertEquals(EstadoReserva.CANCELADA, resp.estado());
        verify(flota).liberar(r.getId().toString());
        verify(reservas, never()).save(any());
        verify(historial, never()).save(any());
    }

    @Test
    void siFlotaFallaAlLiberarLaReservaQuedaCanceladaYSeAvisa() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        doThrow(ReservaException.servicioNoDisponible("ServicioFlota", "caido")).when(flota).liberar(any());

        var ex = error(() -> service.cancelar(r.getId(), null, ADMIN));

        assertEquals("SERVICIO_NO_DISPONIBLE", ex.getCodigo());
        assertEquals(EstadoReserva.CANCELADA, r.getEstado());   // quedo guardada: reintentar completa la liberacion
        verify(reservas).save(r);
    }

    @Test
    void cancelarUnaReservaInexistenteResponde404() {
        var id = UUID.randomUUID();
        when(reservas.findById(id)).thenReturn(Optional.empty());
        assertEquals("RESERVA_NO_ENCONTRADA", error(() -> service.cancelar(id, null, ADMIN)).getCodigo());
    }

    // ---------------------------------------------------------------- estados

    @Test
    void checkOutPasaLaReservaAEnCurso() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        var resp = service.modificar(r.getId(), cambioEstado(EstadoReserva.EN_CURSO), AGENTE_LIMA);
        assertEquals(EstadoReserva.EN_CURSO, resp.estado());
        verify(flota, never()).liberar(any());   // el vehiculo sigue apartado mientras esta en curso
    }

    @Test
    void elOrquestadorPuedeCambiarEstadoConElRolServicio() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        var resp = service.modificar(r.getId(), cambioEstado(EstadoReserva.EN_CURSO),
                new Actor("service-orquestador", "SERVICIO", null));
        assertEquals(EstadoReserva.EN_CURSO, resp.estado());
    }

    @Test
    void devolverUnaReservaEnCursoLaFinalizaYLiberaElVehiculo() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        var resp = service.modificar(r.getId(), cambioEstado(EstadoReserva.FINALIZADA), ADMIN);
        assertEquals(EstadoReserva.FINALIZADA, resp.estado());
        verify(flota).liberar(r.getId().toString());
    }

    @Test
    void noSePuedeSaltarDeConfirmadaAFinalizada() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        assertEquals("TRANSICION_INVALIDA",
                error(() -> service.modificar(r.getId(), cambioEstado(EstadoReserva.FINALIZADA), ADMIN)).getCodigo());
    }

    @Test
    void estadoCanceladaEnModificarEquivaleACancelar() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        var resp = service.modificar(r.getId(), cambioEstado(EstadoReserva.CANCELADA), ADMIN);
        assertEquals(EstadoReserva.CANCELADA, resp.estado());
        verify(flota).liberar(r.getId().toString());
    }

    // ---------------------------------------------------------------- permisos

    @Test
    void elPersonalDeOtraSucursalNoVeNiModificaReservasAjenas() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);   // recojo y devolucion en Lima
        assertEquals("RESERVA_NO_ENCONTRADA", error(() -> service.cancelar(r.getId(), null, AGENTE_CUSCO)).getCodigo());
        assertEquals("RESERVA_NO_ENCONTRADA", error(() -> service.obtener(r.getId(), AGENTE_CUSCO)).getCodigo());
        assertEquals("RESERVA_NO_ENCONTRADA",   // sin sucursal asignada tampoco ve nada
                error(() -> service.cancelar(r.getId(), null, new Actor("a", "AGENTE", null))).getCodigo());
    }

    @Test
    void elAdministradorGeneralModificaCualquierSucursal() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        assertEquals(EstadoReserva.CANCELADA, service.cancelar(r.getId(), null, ADMIN).estado());
    }

    @Test
    void elClienteSoloVeYCancelaSusReservas() {
        Reserva suya = reserva(EstadoReserva.CONFIRMADA);
        assertEquals(suya.getId(), service.obtener(suya.getId(), CLIENTE_ACTOR).idReserva());
        assertEquals(EstadoReserva.CANCELADA, service.cancelar(suya.getId(), null, CLIENTE_ACTOR).estado());

        // Una reserva de otro cliente se ve como si no existiera.
        Reserva ajena = reserva(EstadoReserva.CONFIRMADA);
        when(clientes.porCorreo("otro@hertz.test"))
                .thenReturn(info(UUID.randomUUID()));
        var otro = new Actor("otro@hertz.test", "CLIENTE", null);
        assertEquals("RESERVA_NO_ENCONTRADA", error(() -> service.obtener(ajena.getId(), otro)).getCodigo());
        assertEquals("RESERVA_NO_ENCONTRADA", error(() -> service.cancelar(ajena.getId(), null, otro)).getCodigo());
    }

    @Test
    void elClienteNoPuedeCambiarEstadosSalvoCancelar() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        assertEquals("ROL_NO_AUTORIZADO",
                error(() -> service.modificar(r.getId(), cambioEstado(EstadoReserva.EN_CURSO), CLIENTE_ACTOR)).getCodigo());
    }

    @Test
    void elHistorialEsSoloParaElPersonal() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        when(historial.findByIdReservaOrderByFechaDesc(r.getId())).thenReturn(List.of());
        assertEquals(0, service.consultarHistorial(r.getId(), AGENTE_LIMA).size());
        assertEquals("ROL_NO_AUTORIZADO", error(() -> service.consultarHistorial(r.getId(), CLIENTE_ACTOR)).getCodigo());
    }

    // ---------------------------------------------------------------- reprogramar

    @Test
    void modificarFechasReprogramaElApartadoYRegistraElCambio() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        var nuevoFin = FIN.plusDays(3);

        var resp = service.modificar(r.getId(), new ModificarReservaRequest(null, null, nuevoFin, null, "extiende viaje"), AGENTE_LIMA);

        assertEquals(nuevoFin, resp.fechaFin());
        assertEquals(INI, resp.fechaInicio());
        verify(flota).reprogramar(r.getId().toString(), LIMA, "SUV", INI, nuevoFin);
        ArgumentCaptor<HistorialReserva> h = ArgumentCaptor.forClass(HistorialReserva.class);
        verify(historial).save(h.capture());
        assertEquals("MODIFICADA", h.getValue().getAccion());
        assertNotNull(h.getValue().getDetalle());
    }

    @Test
    void modificarCategoriaSinCupoConservaLaReservaIntacta() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        when(flota.reprogramar(anyString(), anyString(), eq("PREMIUM"), any(), any()))
                .thenThrow(ReservaException.sinDisponibilidad("sin cupo"));

        var ex = error(() -> service.modificar(r.getId(),
                new ModificarReservaRequest("premium", null, null, null, null), AGENTE_LIMA));

        assertEquals("SIN_DISPONIBILIDAD", ex.getCodigo());
        assertEquals("SUV", r.getCategoria());
        verify(reservas, never()).save(any());
    }

    @Test
    void siFallaElGuardadoDeLaReprogramacionSeRevierteElApartado() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        when(reservas.save(any(Reserva.class))).thenThrow(new IllegalStateException("base caida"));

        assertThrows(IllegalStateException.class, () -> service.modificar(r.getId(),
                new ModificarReservaRequest(null, null, FIN.plusDays(2), null, null), AGENTE_LIMA));

        // Una llamada para mover y otra para volver a las fechas originales.
        verify(flota, times(2)).reprogramar(eq(r.getId().toString()), eq(LIMA), eq("SUV"), any(), any());
        verify(flota).reprogramar(r.getId().toString(), LIMA, "SUV", INI, FIN);
    }

    @Test
    void soloSeReprogramaUnaReservaConfirmada() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        assertEquals("TRANSICION_INVALIDA", error(() -> service.modificar(r.getId(),
                new ModificarReservaRequest(null, null, FIN.plusDays(1), null, null), ADMIN)).getCodigo());
    }

    @Test
    void noSeMezclanCambioDeEstadoYDeFechas() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        assertEquals("SOLICITUD_INVALIDA", error(() -> service.modificar(r.getId(),
                new ModificarReservaRequest(null, null, FIN.plusDays(1), EstadoReserva.EN_CURSO, null), ADMIN)).getCodigo());
    }

    @Test
    void modificarConLosMismosValoresNoLlamaAFlota() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        service.modificar(r.getId(), new ModificarReservaRequest("suv", INI, FIN, null, null), ADMIN);
        verify(flota, never()).reprogramar(any(), any(), any(), any(), any());
        verify(reservas, never()).save(any());
    }

    // ---------------------------------------------------------------- listar

    @SuppressWarnings("unchecked")
    @Test
    void listarLimitaElTamanoYDevuelveElTotal() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        when(reservas.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r), org.springframework.data.domain.PageRequest.of(0, 50), 1));

        var pagina = service.listar(null, null, null, null, null, null, -3, 9999, ADMIN);

        assertEquals(1, pagina.total());
        assertEquals(50, pagina.tamano());
        assertEquals(0, pagina.pagina());
        assertEquals(1, pagina.reservas().size());
    }

    @Test
    void listarPorDocumentoDeUnClienteInexistenteDevuelveListaVacia() {
        when(clientes.porDocumento("DNI", "99999999")).thenReturn(Optional.empty());
        var pagina = service.listar("dni", "99999999", null, null, null, null, null, null, AGENTE_LIMA);
        assertEquals(0, pagina.total());
        assertEquals(0, pagina.reservas().size());
    }

    @Test
    void listarPorDocumentoExigeTipoYNumero() {
        assertEquals("SOLICITUD_INVALIDA",
                error(() -> service.listar("DNI", null, null, null, null, null, null, null, AGENTE_LIMA)).getCodigo());
    }

    @SuppressWarnings("unchecked")
    @Test
    void elClienteListaSoloLasSuyasIgnorandoLosFiltrosDeCliente() {
        when(reservas.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        service.listar("DNI", "11111111", UUID.randomUUID(), null, null, null, null, null, CLIENTE_ACTOR);
        verify(clientes).porCorreo("cliente@hertz.test");
        verify(clientes, never()).porDocumento(any(), any());
    }

    // ---------------------------------------------------------------- sucursales

    @Test
    void elPersonalDeSucursalSoloCreaReservasConRecojoEnSuSede() {
        var desdeCusco = new ReservaRequest(CLIENTE, "SUV", "CUSCO_AEROPUERTO", LIMA, INI, FIN, null);
        assertEquals("SUCURSAL_NO_AUTORIZADA", error(() -> service.crear(desdeCusco, AGENTE_LIMA)).getCodigo());
        verify(flota, never()).apartar(any(), any(), any(), any(), any(), any());

        // El administrador general sí puede crearla en cualquier sede.
        assertEquals("CUSCO_AEROPUERTO", service.crear(desdeCusco, ADMIN).sucursalRecojo());
    }

    @Test
    void laDevolucionPuedeSerEnOtraSucursal() {
        var otraDevolucion = new ReservaRequest(CLIENTE, "SUV", LIMA, "CUSCO_AEROPUERTO", INI, FIN, null);
        assertEquals("CUSCO_AEROPUERTO", service.crear(otraDevolucion, AGENTE_LIMA).sucursalDevolucion());
    }

    @Test
    void laSedeDeDevolucionVeLaReservaYRegistraLaDevolucionPeroNoLaCancela() {
        Reserva enCurso = reserva(EstadoReserva.EN_CURSO, "CUSCO_AEROPUERTO");   // sale de Lima, vuelve a Cusco
        assertEquals(enCurso.getId(), service.obtener(enCurso.getId(), AGENTE_CUSCO).idReserva());
        assertEquals(EstadoReserva.FINALIZADA,
                service.modificar(enCurso.getId(), cambioEstado(EstadoReserva.FINALIZADA), AGENTE_CUSCO).estado());

        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA, "CUSCO_AEROPUERTO");
        assertEquals("SUCURSAL_NO_AUTORIZADA", error(() -> service.cancelar(confirmada.getId(), null, AGENTE_CUSCO)).getCodigo());
        assertEquals("SUCURSAL_NO_AUTORIZADA", error(() -> service.modificar(confirmada.getId(),
                cambioEstado(EstadoReserva.EN_CURSO), AGENTE_CUSCO)).getCodigo());
    }

    @SuppressWarnings("unchecked")
    @Test
    void recogenHoyYLaVisibilidadPorSedeSeAplicanComoFiltros() {
        when(reservas.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        var pagina = service.listar(null, null, null, null, null, true, null, null, AGENTE_LIMA);
        assertEquals(0, pagina.total());
        verify(reservas).findAll(any(Specification.class), any(Pageable.class));
    }

    // ---------------------------------------------------------------- limite y licencia

    @Test
    void unClienteNoPuedeTenerMasDeDosReservasActivas() {
        when(reservas.countByIdClienteAndEstadoIn(any(), any())).thenReturn(2L);
        assertEquals("LIMITE_RESERVAS", error(() -> service.crear(pedido(), AGENTE_LIMA)).getCodigo());
        verify(flota, never()).apartar(any(), any(), any(), any(), any(), any());

        when(reservas.countByIdClienteAndEstadoIn(any(), any())).thenReturn(1L);
        service.crear(pedido(), AGENTE_LIMA);   // con una activa todavia puede reservar otra
    }

    @Test
    void sinLicenciaOConLicenciaVencidaNoSeReserva() {
        var sinLicencia = new ClienteInfo(CLIENTE, "DNI", "12345678", "Ana", "Prueba", "a@m.com", "999", null, null);
        when(clientes.obtener(CLIENTE)).thenReturn(sinLicencia);
        assertEquals("LICENCIA_NO_VIGENTE", error(() -> service.crear(pedido(), AGENTE_LIMA)).getCodigo());

        var venceAntes = new ClienteInfo(CLIENTE, "DNI", "12345678", "Ana", "Prueba", "a@m.com", "999", "Q1", FIN.minusDays(1));
        when(clientes.obtener(CLIENTE)).thenReturn(venceAntes);
        assertEquals("LICENCIA_NO_VIGENTE", error(() -> service.crear(pedido(), AGENTE_LIMA)).getCodigo());

        var venceElUltimoDia = new ClienteInfo(CLIENTE, "DNI", "12345678", "Ana", "Prueba", "a@m.com", "999", "Q1", FIN);
        when(clientes.obtener(CLIENTE)).thenReturn(venceElUltimoDia);
        service.crear(pedido(), AGENTE_LIMA);   // vigente justo hasta el ultimo dia: se acepta
        verify(flota, times(1)).apartar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void alargarLaReservaMasAllaDeLaLicenciaSeRechaza() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        when(clientes.obtener(CLIENTE)).thenReturn(
                new ClienteInfo(CLIENTE, "DNI", "12345678", "Ana", "Prueba", "a@m.com", "999", "Q1", FIN.plusDays(1)));

        assertEquals("LICENCIA_NO_VIGENTE", error(() -> service.modificar(r.getId(),
                new ModificarReservaRequest(null, null, FIN.plusDays(5), null, null), AGENTE_LIMA)).getCodigo());
        verify(flota, never()).reprogramar(any(), any(), any(), any(), any());
    }

    @Test
    void laReservaGuardaQuienAlquilo() {
        var r = service.crear(pedido(), AGENTE_LIMA);
        assertEquals("Ana Prueba", r.clienteNombre());
        assertEquals("DNI 12345678", r.clienteDocumento());
        assertEquals("999888777", r.clienteTelefono());
    }

    @Test
    void elClientePuedeElegirUnVehiculoConcretoYSeEnviaLaPlacaAFlota() {
        var elegido = new ReservaRequest(CLIENTE, "SUV", LIMA, LIMA, INI, FIN, " abc-103 ");
        service.crear(elegido, AGENTE_LIMA);
        verify(flota).apartar(anyString(), eq(LIMA), eq("SUV"), eq(INI), eq(FIN), eq("ABC-103"));
    }

    @Test
    void siElVehiculoElegidoYaNoEstaDisponibleNoSeGuardaLaReserva() {
        when(flota.apartar(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(ReservaException.vehiculoElegidoNoDisponible());
        var elegido = new ReservaRequest(CLIENTE, "SUV", LIMA, LIMA, INI, FIN, "ABC-103");
        assertEquals("VEHICULO_ELEGIDO_NO_DISPONIBLE", error(() -> service.crear(elegido, AGENTE_LIMA)).getCodigo());
        verify(reservas, never()).save(any());
    }
}
