package com.hertz.contrato.service;

import com.hertz.contrato.client.ReservaClient;
import com.hertz.contrato.client.ReservaInfo;
import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.EstadoContrato;
import com.hertz.contrato.exception.ContratoException;
import com.hertz.contrato.repository.ContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private ContratoService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContratoRepository.class);
        reservaClient = Mockito.mock(ReservaClient.class);
        service = new ContratoService(repository, reservaClient);
        when(repository.saveAndFlush(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repository.save(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaContratoParaReservaEnCurso() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("EN_CURSO"));

        var response = service.crear(ID_RESERVA, "AGENTE", LIMA, "agente@hertz.test");

        assertEquals(ID_RESERVA, response.idReserva());
        assertEquals(ID_CLIENTE, response.idCliente());
        assertEquals("ABC-103", response.placa());
        assertEquals(EstadoContrato.VIGENTE, response.estado());
        verify(repository).saveAndFlush(any(Contrato.class));
    }

    @Test
    void devuelveContratoExistenteSinConsultarReservaOtraVez() {
        Contrato existente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(existente));

        var response = service.crear(ID_RESERVA, "AGENTE", LIMA, "agente@hertz.test");

        assertEquals(existente.getId(), response.idContrato());
        verify(reservaClient, never()).obtener(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void noCreaContratoSiLaReservaNoEstaEnCurso() {
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.empty());
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("CONFIRMADA"));

        ContratoException error = assertThrows(ContratoException.class,
                () -> service.crear(ID_RESERVA, "AGENTE", LIMA, "agente@hertz.test"));

        assertEquals("RESERVA_NO_DISPONIBLE", error.getCodigo());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void finalizaContratoSoloConReservaFinalizada() {
        Contrato vigente = contrato();
        when(repository.findByIdReserva(ID_RESERVA)).thenReturn(Optional.of(vigente));
        when(reservaClient.obtener(ID_RESERVA)).thenReturn(reserva("FINALIZADA"));

        var response = service.finalizarPorReserva(ID_RESERVA, "AGENTE", CUSCO);

        assertEquals(EstadoContrato.FINALIZADO, response.estado());
        assertFalse(response.fechaDevolucion().isBefore(INICIO));
        verify(repository).save(vigente);
    }

    private static ReservaInfo reserva(String estado) {
        return new ReservaInfo(ID_RESERVA, estado, ID_CLIENTE, LIMA, CUSCO,
                INICIO, FIN, "ABC-103");
    }

    private static Contrato contrato() {
        return new Contrato(UUID.randomUUID(), ID_RESERVA, ID_CLIENTE, "ABC-103",
                LIMA, CUSCO, INICIO, FIN, "agente@hertz.test");
    }
}
