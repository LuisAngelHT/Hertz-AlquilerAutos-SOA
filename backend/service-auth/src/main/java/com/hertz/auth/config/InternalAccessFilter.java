package com.hertz.auth.config;

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

/** Solo los servicios internos (con el secreto compartido) pueden crear usuarios. */
@Component
public class InternalAccessFilter extends OncePerRequestFilter {

    private final byte[] secreto;

    public InternalAccessFilter(@Value("${gateway.secret}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/usuarios");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recibido = request.getHeader("X-Gateway-Secret");
        if (recibido == null || !MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"codigo\":\"ACCESO_NO_AUTORIZADO\",\"mensaje\":"
                    + "\"Solo los servicios internos pueden crear usuarios\",\"timestamp\":\"" + Instant.now() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}