package com.hertz.gateway.filter;

import com.hertz.gateway.ratelimit.TokenBucket;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlotaRateLimitFilterTest {

    private final AtomicInteger llegadas = new AtomicInteger();
    private final GatewayFilterChain chain = exchange -> {
        llegadas.incrementAndGet();
        return Mono.empty();
    };

    private MockServerWebExchange get(String ruta) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(ruta).build());
    }

    @Test
    void laSolicitudNumero201DeDisponibilidadRecibe429() {
        // Reloj fijo: si usara el reloj real, en una PC lenta se repondria una ficha durante el bucle.
        FlotaRateLimitFilter filtro = new FlotaRateLimitFilter(new TokenBucket(200, () -> 0L));

        for (int i = 0; i < 200; i++) {
            MockServerWebExchange ex = get("/api/v1/flota/disponibilidad");
            filtro.filter(ex, chain).block();
            assertNull(ex.getResponse().getStatusCode());
        }
        assertEquals(200, llegadas.get());

        MockServerWebExchange excedida = get("/api/v1/flota/disponibilidad");
        filtro.filter(excedida, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, excedida.getResponse().getStatusCode());
        assertNotNull(excedida.getResponse().getHeaders().getFirst("Retry-After"));
        assertEquals(200, llegadas.get(), "la solicitud excedida no debe llegar al servicio");
    }

    @Test
    void otrasRutasNoSeLimitan() {
        FlotaRateLimitFilter filtro = new FlotaRateLimitFilter(1);

        for (int i = 0; i < 10; i++) {
            MockServerWebExchange ex = get("/api/v1/flota/vehiculos/ABC-103");
            filtro.filter(ex, chain).block();
            assertNull(ex.getResponse().getStatusCode());
        }
        assertEquals(10, llegadas.get());
    }
}
