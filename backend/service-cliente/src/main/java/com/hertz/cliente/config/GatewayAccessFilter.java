package com.hertz.cliente.config;

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
import java.util.Set;

/**
 * Solo acepta solicitudes que vengan del API Gateway (X-Gateway-Secret).
 * Todas exigen un rol válido (X-Rol), salvo el registro público de clientes.
 */
@Component
public class GatewayAccessFilter extends OncePerRequestFilter {

    private static final Set<String> ROLES = Set.of("AGENTE", "ADMINISTRADOR", "ADMINISTRADOR_SUCURSAL", "CLIENTE");

    private final byte[] secreto;

    public GatewayAccessFilter(@Value("${gateway.secret}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recibido = request.getHeader("X-Gateway-Secret");
        if (recibido == null || !MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            escribir(response, "ACCESO_NO_AUTORIZADO", "Solo se aceptan solicitudes del API Gateway");
            return;
        }

        boolean registroPublico = "POST".equals(request.getMethod())
                && "/api/v1/cliente/registro".equals(request.getRequestURI());
        if (!registroPublico) {
            String rol = request.getHeader("X-Rol");
            if (rol == null || !ROLES.contains(rol)) {
                escribir(response, "ROL_NO_PERMITIDO", "El rol del usuario no tiene acceso a este servicio");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void escribir(HttpServletResponse response, String codigo, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje
                + "\",\"timestamp\":\"" + Instant.now() + "\"}");
    }
}