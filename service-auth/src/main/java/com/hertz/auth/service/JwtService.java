package com.hertz.auth.service;

import com.hertz.auth.config.JwtProperties;
import com.hertz.auth.entity.Usuario;
import com.hertz.auth.exception.AuthException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private static final String ISSUER = "hertz-service-auth";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties props) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.props = props;
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }

    public TokenEmitido generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(Duration.ofMinutes(props.expirationMinutes()));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(usuario.getCorreo())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expira);
    }

    public Jwt validar(String token) {
        try {
            return decoder.decode(token);
        } catch (JwtValidationException e) {
            boolean expirado = e.getErrors().stream()
                    .anyMatch(err -> err.getDescription() != null
                            && err.getDescription().toLowerCase().contains("expired"));
            throw expirado ? AuthException.tokenExpirado() : AuthException.tokenInvalido();
        } catch (JwtException e) {
            throw AuthException.tokenInvalido();
        }
    }
}