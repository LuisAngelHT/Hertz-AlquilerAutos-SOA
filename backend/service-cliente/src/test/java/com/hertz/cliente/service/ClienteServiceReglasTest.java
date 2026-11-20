package com.hertz.cliente.service;

import com.hertz.cliente.client.AuthClient;
import com.hertz.cliente.dto.ActualizarPerfilRequest;
import com.hertz.cliente.dto.ClienteRequest;
import com.hertz.cliente.entity.Cliente;
import com.hertz.cliente.entity.TipoDocumento;
import com.hertz.cliente.exception.ClienteException;
import com.hertz.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas nuevas de cliente: mayoría de edad, licencia vigente, listado y perfil propio. Sin base de datos. */
class ClienteServiceReglasTest {

    private ClienteRepository repo;
    private AuthClient auth;
    private ClienteService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(ClienteRepository.class);
        auth = Mockito.mock(AuthClient.class);
        service = new ClienteService(repo, auth);
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ClienteRequest registro(LocalDate nacimiento, LocalDate vencimientoLicencia) {
        return new ClienteRequest(TipoDocumento.DNI, "12345678", "Ana", "Lopez", nacimiento,
                "ana@mail.com", "999888777", "Q12345678", vencimientoLicencia, "clave1234");
    }

    private Cliente cliente() {
        return new Cliente(TipoDocumento.DNI, "12345678", "Ana", "Lopez", LocalDate.of(1990, 1, 1),
                "ana@mail.com", "999888777", "Q12345678", LocalDate.now().plusYears(2));
    }

    @Test
    void unMenorDeEdadNoPuedeRegistrarse() {
        var ex = assertThrows(ClienteException.class,
                () -> service.registrar(registro(LocalDate.now().minusYears(17), LocalDate.now().plusYears(2))));
        assertEquals("CLIENTE_MENOR_DE_EDAD", ex.getCodigo());
        verify(repo, never()).save(any());
    }

    @Test
    void conExactamente18AniosSePuedeRegistrar() {
        service.registrar(registro(LocalDate.now().minusYears(18), LocalDate.now().plusYears(2)));
        verify(repo).save(any(Cliente.class));
    }

    @Test
    void unaLicenciaVencidaNoSeAcepta() {
        var ex = assertThrows(ClienteException.class,
                () -> service.registrar(registro(LocalDate.of(1990, 1, 1), LocalDate.now().minusDays(1))));
        assertEquals("LICENCIA_VENCIDA", ex.getCodigo());
    }

    @Test
    void listarSoloLoPuedeHacerElPersonal() {
        assertEquals("ROL_NO_PERMITIDO", assertThrows(ClienteException.class,
                () -> service.listar(null, null, null, "CLIENTE")).getCodigo());
    }

    @Test
    void listarLimitaElTamanoYNormalizaElTexto() {
        when(repo.buscar(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cliente()), PageRequest.of(0, 50), 1));
        var pagina = service.listar(" Ana ", -4, 9999, "AGENTE");

        ArgumentCaptor<Pageable> p = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).buscar(org.mockito.ArgumentMatchers.eq("%ana%"), p.capture());
        assertEquals(50, p.getValue().getPageSize());
        assertEquals(0, p.getValue().getPageNumber());
        assertEquals(1, pagina.total());
        assertEquals(1, pagina.clientes().size());
    }

    @Test
    void elClienteVeYEditaSuPropioPerfil() {
        Cliente c = cliente();
        when(repo.findByEmailIgnoreCase("ana@mail.com")).thenReturn(Optional.of(c));

        assertEquals("12345678", service.perfilPropio("ana@mail.com", "CLIENTE").numeroDocumento());

        var nuevoVenc = LocalDate.now().plusYears(5);
        var r = service.actualizarPerfilPropio("ana@mail.com", "CLIENTE",
                new ActualizarPerfilRequest(" 911222333 ", "Z9999", nuevoVenc));
        assertEquals("911222333", r.telefono());
        assertEquals("Z9999", r.licenciaConducir());
        assertEquals(nuevoVenc, r.vencimientoLicencia());
    }

    @Test
    void elPerfilPropioSoloEsParaElRolCliente() {
        assertEquals("ROL_NO_PERMITIDO", assertThrows(ClienteException.class,
                () -> service.perfilPropio("ana@mail.com", "AGENTE")).getCodigo());
    }

    @Test
    void elClienteNoPuedeGuardarUnaLicenciaVencida() {
        when(repo.findByEmailIgnoreCase("ana@mail.com")).thenReturn(Optional.of(cliente()));
        assertEquals("LICENCIA_VENCIDA", assertThrows(ClienteException.class,
                () -> service.actualizarPerfilPropio("ana@mail.com", "CLIENTE",
                        new ActualizarPerfilRequest("911222333", "Z9999", LocalDate.now().minusDays(2)))).getCodigo());
    }
}
