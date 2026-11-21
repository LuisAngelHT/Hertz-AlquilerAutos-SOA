package com.hertz.reserva.service;

import com.hertz.reserva.client.ClienteClient;
import com.hertz.reserva.client.ClienteClient.ClienteInfo;
import com.hertz.reserva.client.ContratoClient;
import com.hertz.reserva.client.FlotaClient;
import com.hertz.reserva.client.FlotaClient.Apartado;
import com.hertz.reserva.client.IncidenciaClient;
import com.hertz.reserva.dto.DanoDto;
import com.hertz.reserva.dto.DevolucionDto;
import com.hertz.reserva.dto.EntregaDto;
import com.hertz.reserva.dto.ItemDto;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ContratoClient contratos;
    private IncidenciaClient incidencias;
    private ReservaService service;

    @BeforeEach
    void setUp() {
        reservas = Mockito.mock(ReservaRepository.class);
        historial = Mockito.mock(HistorialReservaRepository.class);
        flota = Mockito.mock(FlotaClient.class);
        clientes = Mockito.mock(ClienteClient.class);
        contratos = Mockito.mock(ContratoClient.class);
        incidencias = Mockito.mock(IncidenciaClient.class);
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        service = new ReservaService(reservas, historial, flota, clientes, contratos, incidencias,
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

    // Revision del vehiculo al entregarlo y al recibirlo (kilometraje, combustible y danos).
    private static final EntregaDto ENTREGA = new EntregaDto(12450, "LLENO",
            List.of(new DanoDto("FRONTAL", "Rayón en el parachoques", "LEVE", null)), "Sin novedades");
    private static final DevolucionDto DEVOLUCION = new DevolucionDto(12980, "MEDIO",
            List.of(new DanoDto("LATERAL_DERECHO", "Abolladura en la puerta", "MODERADO", new BigDecimal("120.50"))),
            null, true);

    // Entregar (EN_CURSO) lleva la revision de salida y la firma del contrato; recibir (FINALIZADA), la de regreso.
    private static ModificarReservaRequest cambioEstado(EstadoReserva e) {
        return new ModificarReservaRequest(null, null, null, e, null,
                e == EstadoReserva.EN_CURSO ? ENTREGA : null,
                e == EstadoReserva.EN_CURSO ? Boolean.TRUE : null,
                e == EstadoReserva.FINALIZADA ? DEVOLUCION : null);
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
        verify(contratos).crear(r.getId(), "agente@hertz.test", ENTREGA);   // el contrato queda a nombre del empleado y con su revision
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
        verify(contratos).finalizar(r.getId(), "admin@hertz.test", DEVOLUCION);
    }

    // El documento: en el check-out el vehículo pasa a ALQUILADO en flota antes de crear el contrato.
    @Test
    void checkOutMarcaElVehiculoAlquiladoAntesDeCrearElContrato() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        service.modificar(r.getId(), cambioEstado(EstadoReserva.EN_CURSO), AGENTE_LIMA);
        var orden = org.mockito.Mockito.inOrder(flota, contratos);
        orden.verify(flota).marcarAlquilado(eq("ABC-103"), anyString());
        orden.verify(contratos).crear(eq(r.getId()), anyString(), any());
    }

    @Test
    void siElVehiculoNoSePuedeEntregarLaReservaQuedaComoEstaba() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        doThrow(ReservaException.transicionInvalida("El vehículo ABC-103 está en MANTENIMIENTO"))
                .when(flota).marcarAlquilado(eq("ABC-103"), anyString());

        assertEquals("TRANSICION_INVALIDA", error(() -> service.modificar(
                r.getId(), cambioEstado(EstadoReserva.EN_CURSO), AGENTE_LIMA)).getCodigo());
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());   // no se tocó
        verify(reservas, never()).save(any());
        verify(contratos, never()).crear(any(), any(), any());
    }

    @Test
    void devolverMarcaElVehiculoDevueltoEnFlota() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        service.modificar(r.getId(), cambioEstado(EstadoReserva.FINALIZADA), ADMIN);
        verify(flota).marcarDevuelto(eq("ABC-103"), anyString());
    }

    @Test
    void repetirLaDevolucionCompletaLosPasosQueQuedaronPendientes() {
        Reserva r = reserva(EstadoReserva.FINALIZADA);
        service.modificar(r.getId(), cambioEstado(EstadoReserva.FINALIZADA), ADMIN);
        verify(flota).marcarDevuelto(eq("ABC-103"), anyString());
        verify(contratos).finalizar(r.getId(), "admin@hertz.test", DEVOLUCION);
        verify(reservas, never()).save(any());
    }

    @Test
    void repetirCheckOutReintentaCrearElContratoSinCambiarLaReserva() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        service.modificar(r.getId(), cambioEstado(EstadoReserva.EN_CURSO), ADMIN);
        verify(contratos).crear(r.getId(), "admin@hertz.test", ENTREGA);
        verify(reservas, never()).save(any());
    }

    @Test
    void repetirCheckOutCompletaLaCreacionDelContratoSiElPrimerIntentoFallo() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        doThrow(ReservaException.servicioNoDisponible("ServicioContrato", "intenta nuevamente"))
                .doNothing().when(contratos).crear(r.getId(), "agente@hertz.test", ENTREGA);

        assertEquals("SERVICIO_NO_DISPONIBLE", error(() -> service.modificar(
                r.getId(), cambioEstado(EstadoReserva.EN_CURSO), AGENTE_LIMA)).getCodigo());
        assertEquals(EstadoReserva.EN_CURSO,
                service.modificar(r.getId(), cambioEstado(EstadoReserva.EN_CURSO), AGENTE_LIMA).estado());
        verify(contratos, times(2)).crear(r.getId(), "agente@hertz.test", ENTREGA);
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

    // ---------------------------------------------------------------- entrega y devolucion: revision obligatoria

    @Test
    void noSeEntregaSinLaRevisionDelVehiculo() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        var sinRevision = new ModificarReservaRequest(null, null, null, EstadoReserva.EN_CURSO, null, null, true, null);

        assertEquals("SOLICITUD_INVALIDA", error(() -> service.modificar(r.getId(), sinRevision, AGENTE_LIMA)).getCodigo());

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        verify(flota, never()).marcarAlquilado(any(), any());   // el vehiculo no sale
        verify(contratos, never()).crear(any(), any(), any());
        verify(reservas, never()).save(any());
    }

    @Test
    void noSeEntregaSinConfirmarQueElContratoFueFirmado() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA);
        for (Boolean firmado : new Boolean[] {false, null}) {
            var sinFirma = new ModificarReservaRequest(null, null, null, EstadoReserva.EN_CURSO, null, ENTREGA, firmado, null);
            assertEquals("SOLICITUD_INVALIDA", error(() -> service.modificar(r.getId(), sinFirma, AGENTE_LIMA)).getCodigo());
        }
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        verify(flota, never()).marcarAlquilado(any(), any());
        verify(contratos, never()).crear(any(), any(), any());
    }

    @Test
    void noSeRecibeElVehiculoSinLaRevisionDeDevolucion() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        var sinRevision = new ModificarReservaRequest(null, null, null, EstadoReserva.FINALIZADA, null);

        assertEquals("SOLICITUD_INVALIDA", error(() -> service.modificar(r.getId(), sinRevision, ADMIN)).getCodigo());

        assertEquals(EstadoReserva.EN_CURSO, r.getEstado());
        verify(flota, never()).marcarDevuelto(any(), any());
        verify(contratos, never()).finalizar(any(), any(), any());
    }

    @Test
    void siElClienteNoEstaConformeLaDevolucionExigeElMotivo() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        var sinMotivo = new ModificarReservaRequest(null, null, null, EstadoReserva.FINALIZADA, null, null, null,
                new DevolucionDto(12980, "MEDIO", List.of(), "  ", false));

        assertEquals("SOLICITUD_INVALIDA", error(() -> service.modificar(r.getId(), sinMotivo, ADMIN)).getCodigo());

        assertEquals(EstadoReserva.EN_CURSO, r.getEstado());
        verify(contratos, never()).finalizar(any(), any(), any());
    }

    @Test
    void siElContratoRechazaLaDevolucionLaReservaYElVehiculoQuedanComoEstaban() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        doThrow(ReservaException.solicitudInvalida("El kilometraje al devolver (100) no puede ser menor al de la entrega (200)"))
                .when(contratos).finalizar(eq(r.getId()), anyString(), any());

        var e = error(() -> service.modificar(r.getId(), cambioEstado(EstadoReserva.FINALIZADA), ADMIN));

        assertEquals("SOLICITUD_INVALIDA", e.getCodigo());
        assertEquals(EstadoReserva.EN_CURSO, r.getEstado());   // sigue en curso
        verify(reservas, never()).save(any());                 // no se guardo ningun cambio
        verify(flota, never()).liberar(any());                 // el vehiculo sigue alquilado
        verify(flota, never()).marcarDevuelto(any(), any());
    }

    @Test
    void elContratoSeCierraAntesDeFinalizarLaReservaYLaRevisionViajaCompleta() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        service.modificar(r.getId(), cambioEstado(EstadoReserva.FINALIZADA), ADMIN);

        var orden = org.mockito.Mockito.inOrder(contratos, reservas, flota);
        orden.verify(contratos).finalizar(r.getId(), "admin@hertz.test", DEVOLUCION);
        orden.verify(reservas).save(any());
        orden.verify(flota).marcarDevuelto(eq("ABC-103"), anyString());
    }

    // ---------------------------------------------------------------- daños de la devolucion -> incidencia -> mantenimiento

    private static ModificarReservaRequest devolverConDanos(DanoDto... danos) {
        return new ModificarReservaRequest(null, null, null, EstadoReserva.FINALIZADA, null, null, null,
                new DevolucionDto(12980, "MEDIO", List.of(danos), null, true));
    }

    @Test
    void devolverConDanosRegistraLaIncidenciaDespuesDeDejarElVehiculoDisponible() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        UUID idContrato = UUID.randomUUID();
        when(contratos.finalizar(eq(r.getId()), anyString(), any())).thenReturn(idContrato);

        service.modificar(r.getId(), devolverConDanos(
                new DanoDto("LATERAL_DERECHO", "Abolladura profunda", "MODERADO", new BigDecimal("350.00")),
                new DanoDto("LUNAS", "Espejo roto", "LEVE", new BigDecimal("180.50"))), ADMIN);

        var orden = org.mockito.Mockito.inOrder(flota, incidencias);
        orden.verify(flota).marcarDevuelto(eq("ABC-103"), anyString());   // primero queda disponible...
        orden.verify(incidencias).registrarDanos(eq("ABC-103"), eq(idContrato), eq("DANO"),
                org.mockito.ArgumentMatchers.argThat(t -> t.contains("Ana Prueba") && t.contains("Lado derecho")
                        && t.contains("Abolladura profunda") && t.contains("S/ 350.00") && t.contains("Lunas y espejos")),
                eq(new BigDecimal("530.50")), eq("admin@hertz.test"));   // ...y recien ahi pasa a mantenimiento
    }

    @Test
    void sinDanosNuevosNoSeRegistraNingunaIncidencia() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        service.modificar(r.getId(), devolverConDanos(), ADMIN);
        verify(incidencias, never()).registrarDanos(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unDanoGraveSeTrataComoSiniestroYLosDemasComoDano() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        service.modificar(r.getId(), devolverConDanos(new DanoDto("FRONTAL", "Choque frontal", "GRAVE", null)), ADMIN);
        verify(incidencias).registrarDanos(eq("ABC-103"), any(), eq("SINIESTRO"), anyString(), eq(null), anyString());
        assertEquals("DANO", ReservaService.tipoIncidencia(List.of(new DanoDto("TECHO", "Rayón", "LEVE", null),
                new DanoDto("INTERIOR", "Mancha", "MODERADO", null))));
    }

    @Test
    void siLaIncidenciaFallaLaDevolucionQuedaRegistradaYSePuedeReintentarSinDuplicar() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        doThrow(ReservaException.servicioNoDisponible("ServicioIncidencia", "intenta nuevamente"))
                .doReturn(true)
                .when(incidencias).registrarDanos(any(), any(), any(), any(), any(), any());
        var pedido = devolverConDanos(new DanoDto("FRONTAL", "Parachoques roto", "MODERADO", new BigDecimal("200")));

        var e = error(() -> service.modificar(r.getId(), pedido, ADMIN));

        assertEquals("SERVICIO_NO_DISPONIBLE", e.getCodigo());
        assertTrue(e.getMessage().contains("La devolución quedó registrada"), e.getMessage());
        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());          // la devolucion no se pierde
        verify(flota).marcarDevuelto(eq("ABC-103"), anyString());

        // Volver a registrar la devolucion completa lo pendiente (el cliente de incidencias evita duplicar).
        assertEquals(EstadoReserva.FINALIZADA, service.modificar(r.getId(), pedido, ADMIN).estado());
        verify(incidencias, times(2)).registrarDanos(eq("ABC-103"), any(), eq("DANO"), anyString(), any(), anyString());
    }

    @Test
    void siFaltaOVuelveDanadoUnElementoNuevoTambienSeRegistraLaIncidencia() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        var inventario = List.of(
                new ItemDto("GATA", "OK", null, false),
                new ItemDto("EXTINTOR", "FALTA", new BigDecimal("90.00"), true),
                new ItemDto("TRIANGULO", "FALTA", null, false));   // ya faltaba al entregar: no es del cliente
        var pedido = new ModificarReservaRequest(null, null, null, EstadoReserva.FINALIZADA, null, null, null,
                new DevolucionDto(12980, "MEDIO", List.of(), null, true, inventario));

        service.modificar(r.getId(), pedido, ADMIN);

        verify(incidencias).registrarDanos(eq("ABC-103"), any(), eq("DANO"),
                org.mockito.ArgumentMatchers.argThat(t -> t.contains("Extintor (falta) - S/ 90.00") && !t.contains("Triángulo")),
                eq(new BigDecimal("90.00")), eq("admin@hertz.test"));
    }

    @Test
    void sinDanosNiElementosNuevosConProblemaNoHayIncidencia() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        var pedido = new ModificarReservaRequest(null, null, null, EstadoReserva.FINALIZADA, null, null, null,
                new DevolucionDto(12980, "MEDIO", List.of(), null, true,
                        List.of(new ItemDto("EXTINTOR", "FALTA", null, false))));   // ya faltaba y no se cobra
        service.modificar(r.getId(), pedido, ADMIN);
        verify(incidencias, never()).registrarDanos(any(), any(), any(), any(), any(), any());
    }

    @Test
    void laDescripcionDeLaIncidenciaEsClaraYNoSuperaElLimite() {
        Reserva r = reserva(EstadoReserva.EN_CURSO);
        String texto = ReservaService.descripcionDanos(r, List.of(
                new DanoDto("POSTERIOR", "Luz trasera rota", "LEVE", new BigDecimal("45.5")),
                new DanoDto("INTERIOR", "Quemadura en el asiento", "MODERADO", null)));
        assertTrue(texto.startsWith("Daños causados en el alquiler de Ana Prueba (reserva " + r.getId().toString().substring(0, 8) + "): "), texto);
        assertTrue(texto.contains("1) Parte trasera (leve): Luz trasera rota - S/ 45.50"), texto);
        assertTrue(texto.contains("2) Interior (moderado): Quemadura en el asiento"), texto);

        var muchos = new java.util.ArrayList<DanoDto>();
        for (int i = 0; i < 30; i++) {
            muchos.add(new DanoDto("OTRO", "Rayón largo número " + i + " ".repeat(5) + "x".repeat(40), "LEVE", new BigDecimal("10")));
        }
        assertEquals(500, ReservaService.descripcionDanos(r, muchos).length());   // acotada a lo que admite Incidencias
    }
}
