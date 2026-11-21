package com.hertz.contrato.entity;

/** Estado de un elemento del inventario del vehiculo. */
public enum EstadoItem {
    OK, FALTA, DANADO;

    /** Menor es mejor: sirve para saber si un elemento empeoro entre la entrega y la devolucion. */
    public int gravedad() {
        return ordinal();
    }
}
