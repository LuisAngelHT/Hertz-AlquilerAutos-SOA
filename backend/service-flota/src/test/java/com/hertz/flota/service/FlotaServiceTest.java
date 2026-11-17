package com.hertz.flota.service;

import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;
import com.hertz.flota.exception.FlotaException;
import com.hertz.flota.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de FlotaService. No usan la base de datos: el repositorio
 * se reemplaza por un mock (Mockito), así que corren en milisegundos y sin conexión.
 */
class FlotaServiceTest {

    private VehiculoRepository repo;
    private FlotaService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(VehiculoRepository.class);
        service = new FlotaService(repo);
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
        var r = service.actualizarEstado("abc-103", EstadoVehiculo.ALQUILADO, "AGENTE");
        assertEquals(EstadoVehiculo.ALQUILADO, r.estado());
    }

    @Test
    void alquiladoNoSePuedeAlquilarDeNuevo() {
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.ALQUILADO)));
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-104", EstadoVehiculo.ALQUILADO, "AGENTE"));
        assertEquals("VEHICULO_NO_DISPONIBLE", ex.getCodigo());
    }

    @Test
    void enMantenimientoNoSePuedeAlquilar() {
        when(repo.findById("LMA-203")).thenReturn(Optional.of(vehiculo("LMA-203", EstadoVehiculo.MANTENIMIENTO)));
        assertThrows(FlotaException.class,
                () -> service.actualizarEstado("LMA-203", EstadoVehiculo.ALQUILADO, "ADMINISTRADOR"));
    }

    @Test
    void alquiladoPuedeVolverADisponible() {
        when(repo.findById("ABC-104")).thenReturn(Optional.of(vehiculo("ABC-104", EstadoVehiculo.ALQUILADO)));
        var r = service.actualizarEstado("ABC-104", EstadoVehiculo.DISPONIBLE, "AGENTE");
        assertEquals(EstadoVehiculo.DISPONIBLE, r.estado());
    }

    @Test
    void placaInexistenteDa404() {
        when(repo.findById("XXX-000")).thenReturn(Optional.empty());
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("XXX-000", EstadoVehiculo.DISPONIBLE, "AGENTE"));
        assertEquals("VEHICULO_NO_ENCONTRADO", ex.getCodigo());
    }

    @Test
    void clienteWebNoPuedeCambiarEstado() {
        var ex = assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-103", EstadoVehiculo.ALQUILADO, "CLIENTE_WEB"));
        assertEquals("ROL_NO_AUTORIZADO", ex.getCodigo());
    }

    @Test
    void sinRolNoPuedeCambiarEstado() {
        assertThrows(FlotaException.class,
                () -> service.actualizarEstado("ABC-103", EstadoVehiculo.ALQUILADO, null));
    }

    @Test
    void fechaFinAnteriorAInicioEsInvalida() {
        var ex = assertThrows(FlotaException.class, () -> service.consultarDisponibilidad(
                "LIMA_AEROPUERTO", "SUV", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1)));
        assertEquals("SOLICITUD_INVALIDA", ex.getCodigo());
    }
}
