package com.hertz.reserva.entity;

/**
 * Estados de una reserva. Transiciones permitidas:
 * CONFIRMADA -> EN_CURSO (check-out) | CANCELADA;  EN_CURSO -> FINALIZADA (devolución).
 * CANCELADA y FINALIZADA son finales.
 */
public enum EstadoReserva {
    CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA;

    public boolean puedePasarA(EstadoReserva destino) {
        return switch (this) {
            case CONFIRMADA -> destino == EN_CURSO || destino == CANCELADA;
            case EN_CURSO -> destino == FINALIZADA;
            case FINALIZADA, CANCELADA -> false;
        };
    }
}
