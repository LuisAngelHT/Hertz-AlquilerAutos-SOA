package com.hertz.gateway.ratelimit;

import java.util.function.LongSupplier;

/**
 * Cubeta de fichas en memoria. Permite como maximo {@code capacidad} solicitudes por minuto:
 * las fichas se reponen de forma continua (capacidad / 60 s) y no pasan de la capacidad.
 * Con varias instancias del gateway el limite es por instancia.
 */
public class TokenBucket {

    private static final long UN_MINUTO_MS = 60_000L;

    private final int capacidad;
    private final LongSupplier reloj;
    private double fichas;
    private long ultimaReposicion;

    public TokenBucket(int capacidad) {
        this(capacidad, System::currentTimeMillis);
    }

    public TokenBucket(int capacidad, LongSupplier reloj) {
        if (capacidad < 1) {
            throw new IllegalArgumentException("La capacidad debe ser al menos 1");
        }
        this.capacidad = capacidad;
        this.reloj = reloj;
        this.fichas = capacidad;
        this.ultimaReposicion = reloj.getAsLong();
    }

    /** Intenta consumir una ficha. Devuelve false si el limite ya se alcanzo. */
    public synchronized boolean intentarConsumir() {
        reponer();
        if (fichas >= 1) {
            fichas -= 1;
            return true;
        }
        return false;
    }

    /** Segundos que faltan para que haya una ficha disponible (minimo 1). */
    public synchronized long segundosParaReintentar() {
        reponer();
        if (fichas >= 1) {
            return 0;
        }
        double msPorFicha = (double) UN_MINUTO_MS / capacidad;
        double faltante = 1 - fichas;
        return Math.max(1, (long) Math.ceil(faltante * msPorFicha / 1000.0));
    }

    private void reponer() {
        long ahora = reloj.getAsLong();
        long transcurrido = Math.max(0, ahora - ultimaReposicion);
        if (transcurrido > 0) {
            fichas = Math.min(capacidad, fichas + transcurrido * ((double) capacidad / UN_MINUTO_MS));
            ultimaReposicion = ahora;
        }
    }
}
