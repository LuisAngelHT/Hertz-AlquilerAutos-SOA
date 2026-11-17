package com.hertz.gateway.filter;

import com.hertz.gateway.exception.ValidacionTokenException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final List<String> RUTAS_PUBLICAS =
        List.of("/api/v1/auth/login", "/api/v1/cliente/registro");
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient authWebClient;
    private final String internalSecret;

    public JwtValidationFilter(WebClient authWebClient,
                            @Value("${gateway.internal-secret}") String internalSecret) {
        this.authWebClient = authWebClient;
        this.internalSecret = internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean esPublica = HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && RUTAS_PUBLICAS.contains(path);
        if (esPublica) {
            ServerHttpRequest publica = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.remove("X-Usuario");   // el cliente no puede inventar identidad
                        h.remove("X-Rol");
                        h.set("X-Gateway-Secret", internalSecret);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(publica).build());
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return escribir(exchange, HttpStatus.UNAUTHORIZED,
                    json("TOKEN_AUSENTE", "Falta el header Authorization: Bearer <token>"));
        }
        String token = header.substring(7).trim();

        return validarToken(token)
                .flatMap(claims -> {
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .headers(h -> {
                                h.set("X-Usuario", String.valueOf(claims.get("correo")));
                                h.set("X-Rol", String.valueOf(claims.get("rol")));
                                h.set("X-Gateway-Secret", internalSecret);
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .onErrorResume(ValidacionTokenException.class,
                        ex -> escribir(exchange, ex.getStatus(), ex.getMessage()));
    }

    private Mono<Map<String, Object>> validarToken(String token) {
        return authWebClient.post()
                .uri("/api/v1/auth/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", token))
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .timeout(Duration.ofSeconds(30))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().is4xxClientError()) {
                        return new ValidacionTokenException(
                                HttpStatus.valueOf(ex.getStatusCode().value()), ex.getResponseBodyAsString());
                    }
                    return servicioNoDisponible();
                })
                .onErrorMap(ex -> !(ex instanceof ValidacionTokenException), ex -> servicioNoDisponible());
    }

    private ValidacionTokenException servicioNoDisponible() {
        return new ValidacionTokenException(HttpStatus.SERVICE_UNAVAILABLE,
                json("SERVICIO_AUTH_NO_DISPONIBLE", "El servicio de autenticación no está disponible"));
    }

    private String json(String codigo, String mensaje) {
        return "{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\",\"timestamp\":\"" + Instant.now() + "\"}";
    }

    private Mono<Void> escribir(ServerWebExchange exchange, HttpStatus status, String cuerpoJson) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(cuerpoJson.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
