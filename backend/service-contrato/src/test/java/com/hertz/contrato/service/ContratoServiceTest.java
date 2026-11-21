package com.hertz.contrato.service;

import com.hertz.contrato.client.ReservaClient;
import com.hertz.contrato.client.ReservaInfo;
import com.hertz.contrato.dto.DanoRequest;
import com.hertz.contrato.dto.DevolucionRequest;
import com.hertz.contrato.dto.DocumentoMeta;
import com.hertz.contrato.dto.InspeccionRequest;
import com.hertz.contrato.dto.ItemInventarioRequest;
import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.Combustible;
import com.hertz.contrato.entity.ContratoDocumento;
import com.hertz.contrato.entity.ContratoInspeccion;
import com.hertz.contrato.entity.EstadoContrato;
import com.hertz.contrato.entity.EstadoItem;
import com.hertz.contrato.entity.ItemInventario;
import com.hertz.contrato.entity.TipoDano;
import com.hertz.contrato.entity.VistaVehiculo;
import com.hertz.contrato.entity.GravedadDano;
import com.hertz.contrato.entity.TipoInspeccion;
import com.hertz.contrato.entity.ZonaVehiculo;
import com.hertz.contrato.exception.ContratoException;
import com.hertz.contrato.repository.ContratoDocumentoRepository;
import com.hertz.contrato.repository.ContratoInspeccionRepository;
import com.hertz.contrato.repository.ContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContratoServiceTest {

    private static final UUID ID_RESERVA = UUID.randomUUID();
    private static final UUID ID_CLIENTE = UUID.randomUUID();
    private static final LocalDate INICIO = LocalDate.of(2025, 1, 10);
    private static final LocalDate FIN = LocalDate.of(2025, 1, 15);
    private static final String LIMA = "LIMA_AEROPUERTO";
    private static final String CUSCO = "CUSCO_AEROPUERTO";

    private ContratoRepository repository;
    private ReservaClient reservaClient;
    private ContratoDocumentoRepository documentos;
    private ContratoInspeccionRepository inspecciones;
    private ContratoService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContratoRepository.class);
        reservaClient = Mockito.mock(ReservaClient.class);
        documentos = Mockito.mock(ContratoDocumentoRepository.class);
        inspecciones = Mockito.mock(ContratoInspeccionRepository.class);
        service = new ContratoService(repository, reservaClient, documentos, inspecciones,
                Mockito.mock(PlatformTransactionManager.class));
        when(inspecciones.saveAndFlush(any(ContratoInspeccion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repository.saveAndFlush(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repository.save(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    // ---------------------------------------------------------------- entrega: contrato + revision + firma

    private static InspeccionRequest salida(int km) {
        return new InspeccionRequest(km, Combustible.LLENO,
                List.of(new DanoRequest(ZonaVehiculo.FRONTAL, "  Rayón en el parachoques  ", GravedadDano.LEVE,
                        new BigDecimal("50.00"))),
                "Tanque lleno, sin novedades");
    }

    private static DanoRequest danoNuevo(String descripcion, String costo) {
        return new DanoRequest(ZonaVehiculo.LATERAL_DERECHO, descripcion, GravedadDano.MODERADO,
                costo == null ? null : new BigDecimal(costo));
    }

    private static DevolucionRequest regreso(int km, Boolean conforme, String observaciones, DanoRequest... danos) {
        return new DevolucionRequest(km, Combustible.MEDIO, List.of(danos), observaciones, conforme);
    }

    private void entregaGuardada(Contrato c, int km) {
        when(inspecciones.findByIdContratoIn(any())).thenReturn(List.of(new ContratoInspeccion(
                c.getId(), TipoInspeccion.ENTREGA, km, Combustible.LLENO, null, null, "agente@hertz.test", List.of())));
    }

    @Test
    void creaContratoConLaRevisionDeSalida() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("EN_CURSO"));

        var response = service.crear(ID_RESERVA, salida(12450), true, "AGENTE", LIMA, "agente@hertz.test");

        assertEquals(ID_RESERVA, response.idReserva());
        assertEquals(ID_CLIENTE, response.idCliente());
        assertEquals("ABC-103", response.placa());
        assertEquals(EstadoContrato.VIGENTE, response.estado());
        assertEquals(12450, response.entrega().kilometraje());
        assertEquals("LLENO", response.entrega().combustible());
        assertEquals("agente@hertz.test", response.entrega().registradoPor());
        assertNull(response.devolucion());
        // El dano que ya tenia queda anotado (sin costo: no se le cobra a nadie) y sin espacios sobrantes.
        assertEquals(1, response.entrega().danos().size());
        assertEquals("Rayón en el parachoques", response.entrega().danos().get(0).descripcion());
        assertNull(response.entrega().danos().get(0).costoEstimado());
        verify(repository).saveAndFlush(any(Contrato.class));
        ArgumentCaptor<ContratoInspeccion> guardada = ArgumentCaptor.forClass(ContratoInspeccion.class);
        verify(inspecciones).saveAndFlush(guardada.capture());
        assertEquals(TipoInspeccion.ENTREGA, guardada.getValue().getTipo());
        assertNull(guardada.getValue().getClienteConforme());
    }

    @Test
    void noSeEntregaSinConfirmarQueElContratoFueFirmado() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        for (Boolean firmado : new Boolean[] {false, null}) {
            var error = assertThrows(ContratoException.class,
                    () -> service.crear(ID_RESERVA, salida(100), firmado, "AGENTE", LIMA, "agente@hertz.test"));
            assertEquals("CONTRATO_NO_FIRMADO", error.getCodigo());
        }
        verify(repository, never()).saveAndFlush(any());
        verify(inspecciones, never()).saveAndFlush(any());
    }

    @Test
    void noSeEntregaSinLaRevisionDelVehiculo() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());

        var error = assertThrows(ContratoException.class,
                () -> service.crear(ID_RESERVA, null, true, "AGENTE", LIMA, "agente@hertz.test"));

        assertEquals("REVISION_INVALIDA", error.getCodigo());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devuelveContratoExistenteSinConsultarReservaOtraVez() {
        Contrato existente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(existente));

        // Repetir la entrega (por ejemplo tras un corte) no exige volver a mandar la revision.
        var response = service.crear(ID_RESERVA, null, null, "AGENTE", LIMA, "agente@hertz.test");

        assertEquals(existente.getId(), response.idContrato());
        verify(reservaClient, never()).obtener(any());
        verify(repository, never()).saveAndFlush(any());
        verify(inspecciones, never()).saveAndFlush(any());
    }

    @Test
    void noCreaContratoSiLaReservaNoEstaEnCurso() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("CONFIRMADA"));

        ContratoException error = assertThrows(ContratoException.class,
                () -> service.crear(ID_RESERVA, salida(100), true, "AGENTE", LIMA, "agente@hertz.test"));

        assertEquals("RESERVA_NO_DISPONIBLE", error.getCodigo());
        verify(repository, never()).saveAndFlush(any());
    }

    // ---------------------------------------------------------------- devolucion: revision y danos a cargo del cliente

    @Test
    void laDevolucionCierraElContratoYCalculaLoQueAsumeElCliente() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("FINALIZADA"));
        entregaGuardada(vigente, 12450);

        var response = service.finalizarPorReserva(ID_RESERVA,
                regreso(12980, true, null, danoNuevo("Abolladura en la puerta", "120.50"), danoNuevo("Espejo roto", "30")),
                "AGENTE", CUSCO, "cusco@hertz.test");

        assertEquals(EstadoContrato.FINALIZADO, response.estado());
        assertFalse(response.fechaDevolucion().isBefore(INICIO));
        assertEquals(12980, response.devolucion().kilometraje());
        assertEquals(Boolean.TRUE, response.devolucion().clienteConforme());
        assertEquals("cusco@hertz.test", response.devolucion().registradoPor());
        assertEquals(2, response.devolucion().danos().size());
        assertEquals(530, response.kilometrosRecorridos());
        assertEquals(new BigDecimal("150.50"), response.totalDanosACargoCliente());
        verify(repository).save(vigente);
        ArgumentCaptor<ContratoInspeccion> guardada = ArgumentCaptor.forClass(ContratoInspeccion.class);
        verify(inspecciones).saveAndFlush(guardada.capture());
        assertEquals(TipoInspeccion.DEVOLUCION, guardada.getValue().getTipo());
    }

    @Test
    void sinDanosNuevosElClienteNoAsumeNada() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("FINALIZADA"));
        entregaGuardada(vigente, 100);

        var response = service.finalizarPorReserva(ID_RESERVA, regreso(150, true, null), "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalDanosACargoCliente()));
        assertTrue(response.devolucion().danos().isEmpty());
    }

    @Test
    void danosSinCostoEstimadoNoSumanPeroQuedanRegistrados() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("FINALIZADA"));
        entregaGuardada(vigente, 100);

        var response = service.finalizarPorReserva(ID_RESERVA,
                regreso(120, true, null, danoNuevo("Rayón", null)), "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(1, response.devolucion().danos().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalDanosACargoCliente()));
    }

    @Test
    void elKilometrajeAlDevolverNoPuedeSerMenorAlDeLaEntrega() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        entregaGuardada(vigente, 5000);

        var error = assertThrows(ContratoException.class, () -> service.finalizarPorReserva(
                ID_RESERVA, regreso(4999, true, null), "AGENTE", CUSCO, "a@hertz.test"));

        assertEquals("REVISION_INVALIDA", error.getCodigo());
        verify(repository, never()).save(any());
        verify(inspecciones, never()).saveAndFlush(any());
    }

    @Test
    void siElClienteNoEstaConformeHayQueExplicarElMotivo() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("FINALIZADA"));
        entregaGuardada(vigente, 100);

        for (String vacio : new String[] {null, "", "   "}) {
            var error = assertThrows(ContratoException.class, () -> service.finalizarPorReserva(
                    ID_RESERVA, regreso(150, false, vacio, danoNuevo("Abolladura", "80")), "AGENTE", CUSCO, "a@hertz.test"));
            assertEquals("REVISION_INVALIDA", error.getCodigo());
        }
        verify(repository, never()).save(any());

        var response = service.finalizarPorReserva(ID_RESERVA,
                regreso(150, false, "El cliente dice que la abolladura ya estaba", danoNuevo("Abolladura", "80")),
                "AGENTE", CUSCO, "a@hertz.test");
        assertEquals(Boolean.FALSE, response.devolucion().clienteConforme());
        assertEquals("El cliente dice que la abolladura ya estaba", response.devolucion().observaciones());
    }

    @Test
    void contratosAnterioresSinRevisionDeSalidaSePuedenCerrarIgual() {
        Contrato antiguo = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(antiguo));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("FINALIZADA"));

        var response = service.finalizarPorReserva(ID_RESERVA, regreso(10, true, null), "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(EstadoContrato.FINALIZADO, response.estado());
        assertNull(response.entrega());
        assertNull(response.kilometrosRecorridos());
    }

    @Test
    void cerrarDosVecesNoDuplicaLaRevisionDeDevolucion() {
        Contrato cerrado = contrato();
        cerrado.finalizar(LocalDate.of(2025, 1, 12));
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(cerrado));

        var response = service.finalizarPorReserva(ID_RESERVA, null, "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(EstadoContrato.FINALIZADO, response.estado());
        verify(inspecciones, never()).saveAndFlush(any());
        verify(repository, never()).save(any());
    }

    @Test
    void elContratoSeCierraConLaReservaAunEnCursoPorqueSeValidaAntesDeFinalizarla() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("EN_CURSO"));

        var response = service.finalizarPorReserva(ID_RESERVA, regreso(10, true, null), "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(EstadoContrato.FINALIZADO, response.estado());
    }

    @Test
    void noSeCierraElContratoDeUnaReservaQueNoSeEntrego() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("CONFIRMADA"));

        var error = assertThrows(ContratoException.class, () -> service.finalizarPorReserva(
                ID_RESERVA, regreso(10, true, null), "AGENTE", CUSCO, "a@hertz.test"));

        assertEquals("TRANSICION_INVALIDA", error.getCodigo());
        verify(inspecciones, never()).saveAndFlush(any());
    }

    // ---------------------------------------------------------------- fechas reales de entrega y devolucion

    private static final java.time.ZoneId LIMA_ZONA = java.time.ZoneId.of("America/Lima");

    @Test
    void elInicioDelContratoEsElDiaRealDeLaEntregaNoElDeLaReserva() {
        LocalDate hoy = LocalDate.now(LIMA_ZONA);
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        // Reserva para dentro de 3 dias que se entrega hoy.
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(new ReservaInfo(ID_RESERVA, "EN_CURSO", ID_CLIENTE,
                LIMA, CUSCO, hoy.plusDays(3), hoy.plusDays(7), "ABC-103"));

        var response = service.crear(ID_RESERVA, salida(100), true, "AGENTE", LIMA, "agente@hertz.test");

        assertEquals(hoy, response.fechaInicio());
        assertEquals(hoy.plusDays(7), response.fechaFinPrevista());
    }

    @Test
    void siSeEntregaDespuesDelFinPrevistoElInicioSeAcotaAlFin() {
        LocalDate hoy = LocalDate.now(LIMA_ZONA);
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(new ReservaInfo(ID_RESERVA, "EN_CURSO", ID_CLIENTE,
                LIMA, CUSCO, hoy.minusDays(10), hoy.minusDays(5), "ABC-103"));

        var response = service.crear(ID_RESERVA, salida(100), true, "AGENTE", LIMA, "agente@hertz.test");

        assertEquals(hoy.minusDays(5), response.fechaInicio());   // nunca posterior al fin previsto
    }

    @Test
    void sePuedeDevolverElMismoDiaAunqueLaReservaFueraParaMasAdelante() {
        LocalDate hoy = LocalDate.now(LIMA_ZONA);
        // Contrato creado hoy con la fecha de la reserva (dentro de 3 dias) como inicio: el caso que fallaba.
        Contrato adelantado = new Contrato(UUID.randomUUID(), ID_RESERVA, ID_CLIENTE, "ABC-103", LIMA, CUSCO,
                hoy.plusDays(3), hoy.plusDays(7), "agente@hertz.test");
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(adelantado));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("EN_CURSO"));
        entregaGuardada(adelantado, 100);

        var response = service.finalizarPorReserva(ID_RESERVA, regreso(120, true, null), "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(EstadoContrato.FINALIZADO, response.estado());
        assertEquals(hoy, response.fechaDevolucion());
        assertEquals(hoy, response.fechaInicio());   // el inicio se corrigio al dia real de la entrega
    }

    // ---------------------------------------------------------------- dibujo del vehiculo e inventario

    private static DanoRequest danoMarcado(String descripcion, VistaVehiculo vista, String x, String y, String costo) {
        return new DanoRequest(ZonaVehiculo.LATERAL_DERECHO, descripcion, GravedadDano.MODERADO,
                costo == null ? null : new BigDecimal(costo), vista, x == null ? null : new BigDecimal(x),
                y == null ? null : new BigDecimal(y), TipoDano.ABOLLADURA);
    }

    private void reservaEnCursoSinContrato() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("EN_CURSO"));
    }

    @Test
    void laEntregaGuardaLaUbicacionDelDanoYElInventarioSinCostos() {
        reservaEnCursoSinContrato();
        var entrega = new InspeccionRequest(1000, Combustible.LLENO,
                List.of(danoMarcado("Abolladura previa", VistaVehiculo.DERECHA, "42.50", "61.00", "99.00")), null,
                List.of(new ItemInventarioRequest(ItemInventario.GATA, EstadoItem.OK, null),
                        new ItemInventarioRequest(ItemInventario.EXTINTOR, EstadoItem.FALTA, new BigDecimal("80"))));

        var response = service.crear(ID_RESERVA, entrega, true, "AGENTE", LIMA, "agente@hertz.test");

        var dano = response.entrega().danos().get(0);
        assertEquals("DERECHA", dano.vista());
        assertEquals(new BigDecimal("42.50"), dano.posX());
        assertEquals(new BigDecimal("61.00"), dano.posY());
        assertEquals("ABOLLADURA", dano.tipo());
        assertNull(dano.costoEstimado());   // en la entrega no se cobra nada
        assertEquals(2, response.entrega().inventario().size());
        assertEquals("EXTINTOR", response.entrega().inventario().get(1).item());
        assertEquals("FALTA", response.entrega().inventario().get(1).estado());
        assertNull(response.entrega().inventario().get(1).costoEstimado());
    }

    @Test
    void elTotalACargoSumaLosDanosNuevosYLoQueFaltaOVuelveDanado() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("EN_CURSO"));
        entregaGuardada(vigente, 100);

        var devolucion = new DevolucionRequest(150, Combustible.MEDIO,
                List.of(danoMarcado("Abolladura", VistaVehiculo.IZQUIERDA, "30", "55", "200.00")), null, true,
                List.of(new ItemInventarioRequest(ItemInventario.GATA, EstadoItem.OK, null),
                        new ItemInventarioRequest(ItemInventario.LLANTA_DE_AUXILIO, EstadoItem.FALTA, new BigDecimal("150.00"))));

        var response = service.finalizarPorReserva(ID_RESERVA, devolucion, "AGENTE", CUSCO, "a@hertz.test");

        assertEquals(new BigDecimal("350.00"), response.totalDanosACargoCliente());   // 200 + 150
        assertEquals("IZQUIERDA", response.devolucion().danos().get(0).vista());
        assertEquals("LLANTA_DE_AUXILIO", response.devolucion().inventario().get(1).item());
        assertEquals(new BigDecimal("150.00"), response.devolucion().inventario().get(1).costoEstimado());
    }

    @Test
    void unDanoMarcadoEnElDibujoNecesitaSuPosicion() {
        reservaEnCursoSinContrato();
        var entrega = new InspeccionRequest(1000, Combustible.LLENO,
                List.of(danoMarcado("Sin posicion", VistaVehiculo.FRENTE, null, "40", null)), null);

        var error = assertThrows(ContratoException.class,
                () -> service.crear(ID_RESERVA, entrega, true, "AGENTE", LIMA, "agente@hertz.test"));

        assertEquals("REVISION_INVALIDA", error.getCodigo());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void sinVistaNoSeGuardaNingunaPosicion() {
        reservaEnCursoSinContrato();
        var entrega = new InspeccionRequest(1000, Combustible.LLENO,
                List.of(danoMarcado("Dano interior", null, "10", "20", null)), null);

        var response = service.crear(ID_RESERVA, entrega, true, "AGENTE", LIMA, "agente@hertz.test");

        assertNull(response.entrega().danos().get(0).vista());
        assertNull(response.entrega().danos().get(0).posX());
        assertNull(response.entrega().danos().get(0).posY());
    }

    @Test
    void unElementoDelInventarioNoPuedeRepetirse() {
        reservaEnCursoSinContrato();
        var entrega = new InspeccionRequest(1000, Combustible.LLENO, List.of(), null,
                List.of(new ItemInventarioRequest(ItemInventario.GATA, EstadoItem.OK, null),
                        new ItemInventarioRequest(ItemInventario.GATA, EstadoItem.FALTA, null)));

        var error = assertThrows(ContratoException.class,
                () -> service.crear(ID_RESERVA, entrega, true, "AGENTE", LIMA, "agente@hertz.test"));

        assertEquals("REVISION_INVALIDA", error.getCodigo());
    }

    @Test
    void noSeCierraSinLaRevisionDelVehiculo() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));

        var error = assertThrows(ContratoException.class,
                () -> service.finalizarPorReserva(ID_RESERVA, null, "AGENTE", CUSCO, "a@hertz.test"));

        assertEquals("REVISION_INVALIDA", error.getCodigo());
    }

    private static ReservaInfo reserva(String estado) {
        return new ReservaInfo(ID_RESERVA, estado, ID_CLIENTE, LIMA, CUSCO,
                INICIO, FIN, "ABC-103");
    }

    private static Contrato contrato() {
        return new Contrato(UUID.randomUUID(), ID_RESERVA, ID_CLIENTE, "ABC-103",
                LIMA, CUSCO, INICIO, FIN, "agente@hertz.test");
    }

    // ---------------------------------------------------------------- contrato firmado escaneado

    private static final byte[] PDF = "%PDF-1.7 contrato firmado".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private Contrato contratoGuardado() {
        Contrato c = contrato();
        when(repository.findById(c.getId())).thenReturn(Optional.of(c));
        when(documentos.findById(c.getId())).thenReturn(Optional.empty());
        when(documentos.saveAndFlush(any(ContratoDocumento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        return c;
    }

    @Test
    void guardaElContratoFirmadoYRegistraQuienLoSubio() {
        Contrato c = contratoGuardado();

        var resp = service.subirDocumento(c.getId(), "AGENTE", LIMA, "agente@hertz.test", "Contrato Ana.pdf", PDF);

        assertEquals("Contrato Ana.pdf", resp.documento().nombreArchivo());
        assertEquals("application/pdf", resp.documento().tipoContenido());
        assertEquals(PDF.length, resp.documento().tamanoBytes());
        assertEquals("agente@hertz.test", resp.documento().subidoPor());
        assertNotNull(resp.documento().subidoEn());
        verify(documentos).saveAndFlush(any(ContratoDocumento.class));
    }

    @Test
    void aceptaImagenesJpgYPngEscaneadas() {
        Contrato c = contratoGuardado();
        assertEquals("image/png", service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "scan.png", PNG)
                .documento().tipoContenido());
        assertEquals("image/jpeg", service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "scan.jpeg", JPG)
                .documento().tipoContenido());
    }

    @Test
    void elTipoSeDeduceDelContenidoNoDeLaExtension() {
        Contrato c = contratoGuardado();
        byte[] ejecutable = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0};

        var error = assertThrows(ContratoException.class, () ->
                service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "contrato.pdf", ejecutable));

        assertEquals("DOCUMENTO_INVALIDO", error.getCodigo());
        verify(documentos, never()).saveAndFlush(any());
    }

    @Test
    void rechazaArchivosVaciosOMuyGrandes() {
        Contrato c = contratoGuardado();
        assertEquals("DOCUMENTO_INVALIDO", assertThrows(ContratoException.class, () ->
                service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "x.pdf", new byte[0])).getCodigo());
        assertEquals("DOCUMENTO_INVALIDO", assertThrows(ContratoException.class, () ->
                service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "x.pdf", null)).getCodigo());

        byte[] grande = new byte[ContratoService.DOCUMENTO_MAX_BYTES + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        var error = assertThrows(ContratoException.class, () ->
                service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "x.pdf", grande));
        assertEquals("DOCUMENTO_MUY_GRANDE", error.getCodigo());

        byte[] justo = new byte[ContratoService.DOCUMENTO_MAX_BYTES];
        System.arraycopy(PDF, 0, justo, 0, PDF.length);
        assertEquals(justo.length, service.subirDocumento(c.getId(), "AGENTE", LIMA, "a@hertz.test", "x.pdf", justo)
                .documento().tamanoBytes());
    }

    @Test
    void soloElPersonalDeHertzPuedeSubirYVerElContrato() {
        Contrato c = contratoGuardado();
        for (String rol : new String[] {"SERVICIO", "CLIENTE", "", null}) {
            assertEquals("ROL_NO_AUTORIZADO", assertThrows(ContratoException.class, () ->
                    service.subirDocumento(c.getId(), rol, LIMA, "x", "x.pdf", PDF)).getCodigo());
            assertEquals("ROL_NO_AUTORIZADO", assertThrows(ContratoException.class, () ->
                    service.obtenerDocumento(c.getId(), rol, LIMA)).getCodigo());
        }
    }

    @Test
    void unaSucursalNoPuedeTocarContratosDeOtra() {
        Contrato c = contratoGuardado();   // recojo LIMA, devolucion CUSCO
        var error = assertThrows(ContratoException.class, () ->
                service.subirDocumento(c.getId(), "AGENTE", "TRUJILLO_CENTRO", "a@hertz.test", "x.pdf", PDF));
        assertEquals("SUCURSAL_NO_AUTORIZADA", error.getCodigo());
        assertEquals("SUCURSAL_NO_AUTORIZADA", assertThrows(ContratoException.class, () ->
                service.obtenerDocumento(c.getId(), "AGENTE", "TRUJILLO_CENTRO")).getCodigo());

        // La sede de devolucion tambien puede, y el administrador general siempre.
        service.subirDocumento(c.getId(), "AGENTE", CUSCO, "a@hertz.test", "x.pdf", PDF);
        service.subirDocumento(c.getId(), "ADMINISTRADOR", null, "admin@hertz.test", "x.pdf", PDF);
    }

    @Test
    void reemplazarElContratoSobrescribeElAnteriorYDejaQuienLoHizo() {
        Contrato c = contratoGuardado();
        ContratoDocumento anterior = new ContratoDocumento(c.getId(), "viejo.pdf", "application/pdf", PDF, "agente@hertz.test");
        when(documentos.findById(c.getId())).thenReturn(Optional.of(anterior));

        var resp = service.subirDocumento(c.getId(), "ADMINISTRADOR", null, "admin@hertz.test", "nuevo.png", PNG);

        assertEquals("nuevo.png", resp.documento().nombreArchivo());
        assertEquals("admin@hertz.test", resp.documento().subidoPor());
        assertArrayEquals(PNG, anterior.getContenido());   // mismo registro, contenido nuevo (no se duplica)
    }

    @Test
    void elNombreSeLimpiaYTomaLaExtensionDelTipoReal() {
        assertEquals("contrato-firmado.pdf", ContratoService.nombreSeguro(null, "application/pdf"));
        assertEquals("contrato-firmado.pdf", ContratoService.nombreSeguro("   ", "application/pdf"));
        assertEquals("passwd.pdf", ContratoService.nombreSeguro("../../etc/passwd", "application/pdf"));
        assertEquals("secreto.jpg", ContratoService.nombreSeguro("C:\\Users\\Ana\\secreto.exe", "image/jpeg"));
        assertEquals("a_b_c_.png", ContratoService.nombreSeguro("a<b>c\".png", "image/png"));
        assertEquals("Contrato Ñandú (1).pdf", ContratoService.nombreSeguro("Contrato Ñandú (1).pdf", "application/pdf"));
        assertEquals(120 + 4, ContratoService.nombreSeguro("x".repeat(500) + ".pdf", "application/pdf").length());
    }

    @Test
    void verElContratoDevuelveElArchivoGuardado() {
        Contrato c = contratoGuardado();
        when(documentos.findById(c.getId())).thenReturn(Optional.of(
                new ContratoDocumento(c.getId(), "firmado.pdf", "application/pdf", PDF, "agente@hertz.test")));

        ContratoDocumento documento = service.obtenerDocumento(c.getId(), "AGENTE", LIMA);

        assertArrayEquals(PDF, documento.getContenido());
        assertEquals("application/pdf", documento.getTipoContenido());
    }

    @Test
    void sinContratoFirmadoSubidoAvisaConClaridad() {
        Contrato c = contratoGuardado();
        assertEquals("DOCUMENTO_NO_ENCONTRADO", assertThrows(ContratoException.class, () ->
                service.obtenerDocumento(c.getId(), "AGENTE", LIMA)).getCodigo());

        UUID inexistente = UUID.randomUUID();
        when(repository.findById(inexistente)).thenReturn(Optional.empty());
        assertEquals("CONTRATO_NO_ENCONTRADO", assertThrows(ContratoException.class, () ->
                service.subirDocumento(inexistente, "AGENTE", LIMA, "a@hertz.test", "x.pdf", PDF)).getCodigo());
    }

    @Test
    void elListadoMarcaQueContratosYaTienenSuEscaneoConUnaSolaConsulta() {
        Contrato conArchivo = contrato();
        Contrato sinArchivo = contrato();
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(conArchivo, sinArchivo)));
        when(documentos.metasPorContratos(any())).thenReturn(List.of(new DocumentoMeta(
                conArchivo.getId(), "firmado.pdf", "application/pdf", 10, "agente@hertz.test", Instant.now())));

        var pagina = service.listar(null, null, null, null, 0, 10, "ADMINISTRADOR", null);

        assertEquals("firmado.pdf", pagina.contratos().get(0).documento().nombreArchivo());
        assertNull(pagina.contratos().get(1).documento());
        verify(documentos, org.mockito.Mockito.times(1)).metasPorContratos(any());
    }
}
