package com.hertz.flota.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Solo acepta solicitudes que vengan del API Gateway: el gateway agrega el header
 * X-Gateway-Secret con un secreto compartido (variable GATEWAY_SECRET, la misma en todos los
 * servicios). Sin ese header, o con uno distinto, se responde 403 ACCESO_NO_AUTORIZADO.
 * Es el mismo mecanismo que usa service-cliente. Así nadie puede llamar a flota directo
 * (saltándose la validación del token) y falsificar los headers X-Rol y X-Usuario.
 */
@Component
public class GatewayAccessFilter extends OncePerRequestFilter {

    private final byte[] secreto;

    public GatewayAccessFilter(@Value("${gateway.secret}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    // El health check (Render) llama sin pasar por el gateway.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recibido = request.getHeader("X-Gateway-Secret");
        // MessageDigest.isEqual compara en tiempo constante (evita ataques por tiempo de respuesta).
        if (recibido == null || !MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"codigo\":\"ACCESO_NO_AUTORIZADO\","
                    + "\"mensaje\":\"Solo se aceptan solicitudes del API Gateway\","
                    + "\"timestamp\":\"" + Instant.now() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
