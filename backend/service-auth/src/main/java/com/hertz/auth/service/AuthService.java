package com.hertz.auth.service;

import com.hertz.auth.dto.*;
import com.hertz.auth.entity.Rol;
import com.hertz.auth.entity.Usuario;
import com.hertz.auth.exception.AuthException;
import com.hertz.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    // Tras este número de contraseñas incorrectas seguidas la cuenta se bloquea este tiempo.
    static final int MAX_INTENTOS = 5;
    static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Inicia sesión. Si la contraseña es incorrecta cuenta el intento; al llegar a MAX_INTENTOS
     * bloquea la cuenta DURACION_BLOQUEO. Mientras está bloqueada rechaza incluso la contraseña
     * correcta. noRollbackFor: el contador debe guardarse aunque el login falle (si no, la
     * transacción se revertiría y nunca se acumularían intentos).
     */
    @Transactional(noRollbackFor = AuthException.class)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .orElseThrow(AuthException::credencialesInvalidas);

        Instant ahora = Instant.now();
        if (usuario.estaBloqueada(ahora)) {
            long minutos = Math.max(1, (Duration.between(ahora, usuario.getBloqueadoHasta()).toSeconds() + 59) / 60);
            throw AuthException.cuentaBloqueada(minutos);
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            usuario.registrarIntentoFallido(MAX_INTENTOS, ahora.plus(DURACION_BLOQUEO));
            if (usuario.estaBloqueada(ahora)) {
                throw AuthException.cuentaBloqueada(DURACION_BLOQUEO.toMinutes());
            }
            throw AuthException.credencialesInvalidas(MAX_INTENTOS - usuario.getIntentosFallidos());
        }
        if (!usuario.isActivo()) {
            throw AuthException.usuarioInactivo();
        }

        usuario.limpiarIntentos();
        usuario.setUltimoLogin(ahora);
        JwtService.TokenEmitido emitido = jwtService.generar(usuario);

        return new LoginResponse(emitido.token(), "Bearer", emitido.expiraEn(),
                usuario.getRol().name(), usuario.getNombre(), usuario.getSucursal());
    }

    public ValidarTokenResponse validarToken(String token) {
        Jwt jwt = jwtService.validar(token);
        return new ValidarTokenResponse(true, jwt.getSubject(), jwt.getClaimAsString("rol"), jwt.getExpiresAt(),
                jwt.getClaimAsString("sucursal"));
    }

    @Transactional(readOnly = true)
    public PerfilResponse perfil(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw AuthException.tokenAusente();
        }
        Jwt jwt = jwtService.validar(authorizationHeader.substring(7).trim());
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(jwt.getSubject())
                .orElseThrow(AuthException::tokenInvalido);
        return new PerfilResponse(usuario.getCorreo(), usuario.getNombre(), usuario.getRol().name());
    }
    
    @Transactional
    public UsuarioCreadoResponse crearUsuarioCliente(CrearUsuarioRequest request) {
        String correo = request.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw AuthException.correoYaRegistrado();
        }
        Usuario usuario = new Usuario(correo, request.nombre().trim(),
                passwordEncoder.encode(request.password()), Rol.CLIENTE, request.clienteId());
        Usuario guardado = usuarioRepository.save(usuario);
        return new UsuarioCreadoResponse(guardado.getId(), guardado.getCorreo(), guardado.getRol().name());
    }
}