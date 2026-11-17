package com.hertz.gateway.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    @Test
    void permiteExactamente200EnUnMinutoYBloqueaLaSiguiente() {
        AtomicLong reloj = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(200, reloj::get);

        int permitidas = 0;
        for (int i = 0; i < 250; i++) {
            if (bucket.intentarConsumir()) {
                permitidas++;
            }
        }
        assertEquals(200, permitidas);
        assertFalse(bucket.intentarConsumir());
    }

    @Test
    void sePuedeConsultarOtraVezCuandoPasaElTiempo() {
        AtomicLong reloj = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(200, reloj::get);
        for (int i = 0; i < 200; i++) {
            bucket.intentarConsumir();
        }
        assertFalse(bucket.intentarConsumir());

        reloj.addAndGet(300); // 200 por minuto = una ficha cada 300 ms
        assertTrue(bucket.intentarConsumir());
        assertFalse(bucket.intentarConsumir());
    }

    @Test
    void despuesDeUnMinutoCompletoVuelveALaCapacidadSinPasarla() {
        AtomicLong reloj = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(5, reloj::get);
        for (int i = 0; i < 5; i++) {
            bucket.intentarConsumir();
        }
        reloj.addAndGet(10 * 60_000L); // 10 minutos despues

        int permitidas = 0;
        for (int i = 0; i < 20; i++) {
            if (bucket.intentarConsumir()) {
                permitidas++;
            }
        }
        assertEquals(5, permitidas);
    }

    @Test
    void indicaCuantoEsperarCuandoSeAgota() {
        AtomicLong reloj = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(60, reloj::get); // una ficha por segundo
        for (int i = 0; i < 60; i++) {
            bucket.intentarConsumir();
        }
        assertEquals(1, bucket.segundosParaReintentar());
    }

    @Test
    void rechazaCapacidadInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0));
    }
}
