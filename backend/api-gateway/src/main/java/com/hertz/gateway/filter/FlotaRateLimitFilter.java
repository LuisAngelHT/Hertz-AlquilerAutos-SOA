package com.hertz.gateway.filter;

import com.hertz.gateway.ratelimit.TokenBucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Throttling del API Gateway: limita las consultas de disponibilidad de ServicioFlota
 * (por defecto a 200 por minuto) para proteger la base de datos de flota.
 * Corre despues de JwtValidationFilter, asi solo cuentan las solicitudes autenticadas.
 */
@Component
public class FlotaRateLimitFilter implements GlobalFilter, Ordered {

    static final String RUTA_DISPONIBILIDAD = "/api/v1/flota/disponibilidad";

    private final TokenBucket bucket;

    @Autowired
    public FlotaRateLimitFilter(
            @Value("${rate-limit.flota-disponibilidad.por-minuto:200}") int porMinuto) {
        this.bucket = new TokenBucket(porMinuto);
    }

    // Solo para pruebas: permite inyectar una cubeta con reloj controlado (sin depender del tiempo real).
    FlotaRateLimitFilter(TokenBucket bucket) {
        this.bucket = bucket;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!RUTA_DISPONIBILIDAD.equals(path) || bucket.intentarConsumir()) {
            return chain.filter(exchange);
        }
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", String.valueOf(bucket.segundosParaReintentar()));
        String cuerpo = "{\"codigo\":\"LIMITE_EXCEDIDO\",\"mensaje\":\"Se supero el limite de consultas de "
                + "disponibilidad por minuto. Intenta de nuevo en unos segundos\",\"timestamp\":\""
                + Instant.now() + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
