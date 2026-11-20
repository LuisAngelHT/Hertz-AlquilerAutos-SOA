package com.hertz.incidencia.service;

import com.hertz.incidencia.client.FlotaClient;
import com.hertz.incidencia.client.VehiculoInfo;
import com.hertz.incidencia.entity.EstadoIncidencia;
import com.hertz.incidencia.entity.Incidencia;
import com.hertz.incidencia.entity.TipoIncidencia;
import com.hertz.incidencia.exception.IncidenciaException;
import com.hertz.incidencia.repository.IncidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class IncidenciaServiceTest {

    private static final String PLACA = "ABC-104";
    private static final String SUCURSAL = "LIMA_AEROPUERTO";

    private IncidenciaRepository repository;
    private FlotaClient flotaClient;
    private IncidenciaService service;

    @BeforeEach
    void setUp() {
        repository = mock(IncidenciaRepository.class);
        flotaClient = mock(FlotaClient.class);
        service = new IncidenciaService(repository, flotaClient);
    }

    private Incidencia incidencia(UUID id) {
        return new Incidencia(id, PLACA, SUCURSAL, null, TipoIncidencia.MANTENIMIENTO,
                "Revision del vehiculo", null, "DISPONIBLE", "agente@hertz.pe");
    }

    @Test
    void cerrarUltimaIncidenciaLiberaElVehiculoYGuardaElCierre() {
        UUID id = UUID.randomUUID();
        Incidencia incidencia = incidencia(id);
        when(repository.findById(id)).thenReturn(Optional.of(incidencia));
        when(repository.existsByPlacaAndEstadoAndIdNot(PLACA, EstadoIncidencia.ABIERTA, id))
                .thenReturn(false);
        when(flotaClient.obtenerVehiculo(PLACA))
                .thenReturn(new VehiculoInfo(PLACA, "MANTENIMIENTO", SUCURSAL));

        var resultado = service.cerrar(id, SUCURSAL);

        assertEquals(EstadoIncidencia.CERRADA, resultado.estado());
        assertEquals("DISPONIBLE", resultado.estadoVehiculo());
        verify(repository).save(incidencia);
        verify(flotaClient).pasarADisponible(PLACA, "Incidencia " + id + " cerrada");
    }

    @Test
    void cerrarIncidenciaNoLiberaElVehiculoSiQuedaOtraAbierta() {
        UUID id = UUID.randomUUID();
        Incidencia incidencia = incidencia(id);
        when(repository.findById(id)).thenReturn(Optional.of(incidencia));
        when(repository.existsByPlacaAndEstadoAndIdNot(PLACA, EstadoIncidencia.ABIERTA, id))
                .thenReturn(true);
        when(flotaClient.obtenerVehiculo(PLACA))
                .thenReturn(new VehiculoInfo(PLACA, "MANTENIMIENTO", SUCURSAL));

        var resultado = service.cerrar(id, SUCURSAL);

        assertEquals(EstadoIncidencia.CERRADA, resultado.estado());
        assertEquals("MANTENIMIENTO", resultado.estadoVehiculo());
        verify(repository).save(incidencia);
        verify(flotaClient, never()).pasarADisponible(anyString(), anyString());
    }

    @Test
    void cerrarIncidenciaDeOtraSucursalSeRechaza() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(incidencia(id)));

        var ex = assertThrows(IncidenciaException.class, () -> service.cerrar(id, "CUSCO_AEROPUERTO"));

        assertEquals("INCIDENCIA_NO_ENCONTRADA", ex.getCodigo());
        verifyNoInteractions(flotaClient);
        verify(repository, never()).save(any(Incidencia.class));
    }
}
