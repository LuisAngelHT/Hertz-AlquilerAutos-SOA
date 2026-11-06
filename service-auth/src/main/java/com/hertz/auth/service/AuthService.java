package com.hertz.auth.service;

import com.hertz.auth.dto.*;
import com.hertz.auth.entity.Usuario;
import com.hertz.auth.exception.AuthException;
import com.hertz.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .orElseThrow(AuthException::credencialesInvalidas);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw AuthException.credencialesInvalidas();
        }
        if (!usuario.isActivo()) {
            throw AuthException.usuarioInactivo();
        }

        usuario.setUltimoLogin(Instant.now());
        JwtService.TokenEmitido emitido = jwtService.generar(usuario);

        return new LoginResponse(emitido.token(), "Bearer", emitido.expiraEn(),
                usuario.getRol().name(), usuario.getNombre());
    }

    public ValidarTokenResponse validarToken(String token) {
        Jwt jwt = jwtService.validar(token);
        return new ValidarTokenResponse(true, jwt.getSubject(), jwt.getClaimAsString("rol"), jwt.getExpiresAt());
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
}