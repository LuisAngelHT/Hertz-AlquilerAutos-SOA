package com.hertz.incidencia.service;

import com.hertz.incidencia.client.FlotaClient;
import com.hertz.incidencia.client.VehiculoInfo;
import com.hertz.incidencia.dto.IncidenciaRequest;
import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.entity.Incidencia;
import com.hertz.incidencia.entity.TipoIncidencia;
import com.hertz.incidencia.exception.IncidenciaException;
import com.hertz.incidencia.repository.IncidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de IncidenciaService sin base de datos ni red: repositorio y cliente de flota son mocks. */
class IncidenciaServiceTest {

    private static final String LIMA = "LIMA_AEROPUERTO";

    private IncidenciaRepository repository;
    private FlotaClient flota;
    private IncidenciaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(IncidenciaRepository.class);
        flota = Mockito.mock(FlotaClient.class);
        service = new IncidenciaService(repository, flota);
        when(repository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static IncidenciaRequest pedido(String placa) {
        return new IncidenciaRequest(placa, null, TipoIncidencia.DAÑO, "Rayón en la puerta", null);
    }

    private static Incidencia abierta(String placa) {
        return new Incidencia(UUID.randomUUID(), placa, LIMA, null, TipoIncidencia.DAÑO,
                "Rayón", null, "DISPONIBLE", "agente@hertz.test");
    }

    // ---------------------------------------------------------------- registrar

    @Test
    void registrarEnviaElVehiculoDisponibleAMantenimiento() {
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "DISPONIBLE", LIMA));
        var resp = service.registrar(pedido("abc-103"), "agente@hertz.test", LIMA);
        assertEquals(EstadoIncidencia.ABIERTA, resp.estado());
        verify(flota).pasarAMantenimiento(eq("ABC-103"), anyString());
    }

    @Test
    void noSePuedeRegistrarUnaIncidenciaSobreUnVehiculoAlquilado() {
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "ALQUILADO", LIMA));
        var error = assertThrows(IncidenciaException.class,
                () -> service.registrar(pedido("ABC-103"), "agente@hertz.test", LIMA));
        assertEquals("VEHICULO_ALQUILADO", error.getCodigo());
        verify(flota, never()).pasarAMantenimiento(anyString(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void siElVehiculoYaEstabaEnMantenimientoNoSeRepiteElCambio() {
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "MANTENIMIENTO", LIMA));
        service.registrar(pedido("ABC-103"), "agente@hertz.test", LIMA);
        verify(flota, never()).pasarAMantenimiento(anyString(), anyString());
    }

    @Test
    void elPersonalDeOtraSedeNoPuedeRegistrar() {
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "DISPONIBLE", LIMA));
        var error = assertThrows(IncidenciaException.class,
                () -> service.registrar(pedido("ABC-103"), "cusco@hertz.test", "CUSCO_AEROPUERTO"));
        assertEquals("SUCURSAL_NO_AUTORIZADA", error.getCodigo());
    }

    // ---------------------------------------------------------------- cerrar

    @Test
    void cerrarLaUltimaIncidenciaAbiertaDevuelveElVehiculoADisponible() {
        Incidencia i = abierta("ABC-103");
        when(repository.findById(i.getId())).thenReturn(Optional.of(i));
        when(repository.countByPlacaAndEstado("ABC-103", EstadoIncidencia.ABIERTA)).thenReturn(0L);
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "MANTENIMIENTO", LIMA));

        var resp = service.cerrar(i.getId(), LIMA);

        assertEquals(EstadoIncidencia.CERRADA, resp.estado());
        verify(flota).pasarADisponible(eq("ABC-103"), anyString());
    }

    @Test
    void siQuedanOtrasIncidenciasAbiertasElVehiculoSigueEnMantenimiento() {
        Incidencia i = abierta("ABC-103");
        when(repository.findById(i.getId())).thenReturn(Optional.of(i));
        when(repository.countByPlacaAndEstado("ABC-103", EstadoIncidencia.ABIERTA)).thenReturn(1L);

        var resp = service.cerrar(i.getId(), LIMA);

        assertEquals(EstadoIncidencia.CERRADA, resp.estado());
        verify(flota, never()).pasarADisponible(anyString(), anyString());
    }

    @Test
    void cerrarDosVecesEsSeguroYTerminaDeLiberarElVehiculo() {
        Incidencia i = abierta("ABC-103");
        i.cerrar();   // un intento anterior la cerró pero flota no respondió
        when(repository.findById(i.getId())).thenReturn(Optional.of(i));
        when(repository.countByPlacaAndEstado("ABC-103", EstadoIncidencia.ABIERTA)).thenReturn(0L);
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "MANTENIMIENTO", LIMA));

        service.cerrar(i.getId(), LIMA);

        verify(repository, never()).save(any());   // no se vuelve a guardar
        verify(flota).pasarADisponible(eq("ABC-103"), anyString());
    }

    @Test
    void siElVehiculoYaNoEstaEnMantenimientoNoSeToca() {
        Incidencia i = abierta("ABC-103");
        when(repository.findById(i.getId())).thenReturn(Optional.of(i));
        when(repository.countByPlacaAndEstado("ABC-103", EstadoIncidencia.ABIERTA)).thenReturn(0L);
        when(flota.obtenerVehiculo("ABC-103")).thenReturn(new VehiculoInfo("ABC-103", "DISPONIBLE", LIMA));

        service.cerrar(i.getId(), LIMA);

        verify(flota, never()).pasarADisponible(anyString(), anyString());
    }

    @Test
    void cerrarUnaIncidenciaDeOtraSedeEsComoSiNoExistiera() {
        Incidencia i = abierta("ABC-103");
        when(repository.findById(i.getId())).thenReturn(Optional.of(i));
        var error = assertThrows(IncidenciaException.class, () -> service.cerrar(i.getId(), "CUSCO_AEROPUERTO"));
        assertEquals("INCIDENCIA_NO_ENCONTRADA", error.getCodigo());
        verify(repository, never()).save(any());
    }

    @Test
    void cerrarUnaIncidenciaInexistenteDaNoEncontrada() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        var error = assertThrows(IncidenciaException.class, () -> service.cerrar(id, null));
        assertEquals("INCIDENCIA_NO_ENCONTRADA", error.getCodigo());
    }
}
