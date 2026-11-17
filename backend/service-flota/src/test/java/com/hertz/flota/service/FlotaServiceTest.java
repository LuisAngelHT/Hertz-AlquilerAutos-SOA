package com.hertz.flota.service;

import com.hertz.flota.entity.EstadoVehiculo;
import com.hertz.flota.entity.Vehiculo;
import com.hertz.flota.exception.FlotaException;
import com.hertz.flota.entity.HistorialVehiculo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private FlotaService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(VehiculoRepository.class);
        historial = Mockito.mock(HistorialRepository.class);
        service = new FlotaService(repo, historial);
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
        when(repo.listar(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE))));
        var pagina = service.listar(null, null, " ", null, null);
        assertEquals(1, pagina.vehiculos().size());
        assertEquals(1, pagina.total());
        assertEquals(0, pagina.pagina());
        assertEquals(12, pagina.tamano());
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", PageRequest.of(0, 12));
    }

    @Test
    void listarNormalizaSucursalYTexto() {
        when(repo.listar(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        service.listar(EstadoVehiculo.ALQUILADO, " lima_aeropuerto ", " Toyota ", 0, 12);
        verify(repo).listar(EstadoVehiculo.ALQUILADO, "LIMA_AEROPUERTO", "%toyota%", PageRequest.of(0, 12));
    }

    @Test
    void listarLimitaElTamanoDePaginaYNoAceptaPaginasNegativas() {
        when(repo.listar(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        service.listar(null, null, null, -5, 9999);
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", PageRequest.of(0, 50));
        service.listar(null, null, null, 2, 0);
        verify(repo).listar(EstadoVehiculo.DISPONIBLE, "%", "%", PageRequest.of(2, 1));
    }

    @Test
    void listarInformaElTotalYLasPaginas() {
        var vehiculos = List.of(vehiculo("ABC-103", EstadoVehiculo.DISPONIBLE));
        when(repo.listar(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(vehiculos, PageRequest.of(1, 1), 5));
        var pagina = service.listar(null, null, null, 1, 1);
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
}
