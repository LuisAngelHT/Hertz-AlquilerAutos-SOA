package com.hertz.flota.entity;

/**
 * Estados posibles de un vehículo. Son los valores comunes que usa todo el sistema; sirven
 * para ocultar cómo guarde cada sistema legado su propio estado.
 */
public enum EstadoVehiculo {
    /** Se puede reservar o alquilar. */
    DISPONIBLE,
    /** Está con un cliente (contrato vigente). */
    ALQUILADO,
    /** Fuera de servicio por daño, siniestro o mantenimiento; no aparece como disponible. */
    MANTENIMIENTO
}
