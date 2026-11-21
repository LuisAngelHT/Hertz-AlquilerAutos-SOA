package com.hertz.contrato.entity;

/** Nivel de combustible al entregar o recibir el vehiculo. */
public enum Combustible {
    VACIO, UN_CUARTO, MEDIO, TRES_CUARTOS, LLENO;

    /** 0 (vacio) a 4 (lleno): sirve para comparar lo entregado con lo devuelto. */
    public int cuartos() {
        return ordinal();
    }
}
