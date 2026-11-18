package com.hertz.auth.service;

import com.hertz.auth.dto.LoginRequest;
import com.hertz.auth.entity.Rol;
import com.hertz.auth.entity.Usuario;
import com.hertz.auth.exception.AuthException;
import com.hertz.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Pruebas del bloqueo de cuenta por intentos fallidos de login. Sin base de datos. */
class AuthServiceLoginTest {

    private static final String CORREO = "agente@hertz.test";
    private static final String CLAVE = "clave-de-prueba-unitaria";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UsuarioRepository repo;
    private JwtService jwt;
    private AuthService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(UsuarioRepository.class);
        jwt = Mockito.mock(JwtService.class);
        usuario = new Usuario(CORREO, "Agente", encoder.encode(CLAVE), Rol.AGENTE, null);
        when(repo.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(jwt.generar(any())).thenReturn(new JwtService.TokenEmitido("token", Instant.now().plusSeconds(3600)));
        service = new AuthService(repo, encoder, jwt);
    }

    private AuthException fallar(String clave) {
        return assertThrows(AuthException.class, () -> service.login(new LoginRequest(CORREO, clave)));
    }

    @Test
    void unLoginCorrectoEntra() {
        assertEquals("AGENTE", service.login(new LoginRequest(CORREO, CLAVE)).rol());
    }

    @Test
    void unaClaveIncorrectaAvisaCuantosIntentosQuedan() {
        AuthException ex = fallar("mala");
        assertEquals("CREDENCIALES_INVALIDAS", ex.getCodigo());
        assertTrue(ex.getMessage().contains("4 intentos"));
        assertEquals(1, usuario.getIntentosFallidos());
    }

    @Test
    void alQuintoIntentoFallidoLaCuentaSeBloquea() {
        for (int i = 0; i < 4; i++) {
            assertEquals("CREDENCIALES_INVALIDAS", fallar("mala").getCodigo());
        }
        AuthException quinto = fallar("mala");
        assertEquals("CUENTA_BLOQUEADA", quinto.getCodigo());
        assertEquals(423, quinto.getStatus().value());
        assertTrue(usuario.estaBloqueada(Instant.now()));
    }

    @Test
    void conLaCuentaBloqueadaTambienSeRechazaLaClaveCorrecta() {
        for (int i = 0; i < 5; i++) {
            assertThrows(AuthException.class, () -> service.login(new LoginRequest(CORREO, "mala")));
        }
        AuthException ex = fallar(CLAVE);
        assertEquals("CUENTA_BLOQUEADA", ex.getCodigo());
        assertTrue(ex.getMessage().contains("minutos"));
    }

    @Test
    void pasadoElBloqueoSePuedeEntrarDeNuevo() {
        for (int i = 0; i < 5; i++) {
            assertThrows(AuthException.class, () -> service.login(new LoginRequest(CORREO, "mala")));
        }
        // Se simula que ya pasaron los 15 minutos moviendo el fin del bloqueo al pasado.
        usuario.registrarIntentoFallido(1, Instant.now().minusSeconds(60));
        assertFalse(usuario.estaBloqueada(Instant.now()));

        assertEquals("AGENTE", service.login(new LoginRequest(CORREO, CLAVE)).rol());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void unLoginCorrectoReiniciaLosIntentosFallidos() {
        fallar("mala");
        fallar("mala");
        assertEquals(2, usuario.getIntentosFallidos());
        service.login(new LoginRequest(CORREO, CLAVE));
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void uncorreoInexistenteSigueDandoCredencialesInvalidas() {
        when(repo.findByCorreoIgnoreCase("nadie@hertz.test")).thenReturn(Optional.empty());
        AuthException ex = assertThrows(AuthException.class,
                () -> service.login(new LoginRequest("nadie@hertz.test", "x")));
        assertEquals("CREDENCIALES_INVALIDAS", ex.getCodigo());
    }
}
