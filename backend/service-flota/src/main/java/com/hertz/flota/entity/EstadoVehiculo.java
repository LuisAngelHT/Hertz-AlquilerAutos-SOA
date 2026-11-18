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
    MANTENIMIENTO,
    /** Dado de baja (vendido, siniestro total, fin de vida útil). No se borra: conserva su historial. Solo lo cambia el ADMINISTRADOR general. */
    FUERA_DE_SERVICIO
}
