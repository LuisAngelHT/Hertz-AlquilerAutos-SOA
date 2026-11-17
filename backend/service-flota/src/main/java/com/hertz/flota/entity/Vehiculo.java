package com.hertz.flota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Un vehículo de la flota. Se guarda en la tabla flota.vehiculo.
 * La placa es la clave y hace de "idVehiculo" en toda la API.
 * No tiene setters: el único cambio permitido es de estado, con {@link #cambiarEstado}.
 */
@Entity
@Table(name = "vehiculo", schema = "flota")
public class Vehiculo {

    @Id
    @Column(name = "placa", length = 10)
    private String placa;

    @Column(name = "categoria", nullable = false, length = 20)
    private String categoria;

    @Column(name = "marca", nullable = false, length = 40)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 40)
    private String modelo;

    // short y no int porque la columna es smallint; así Hibernate (ddl-auto: validate) no se queja.
    @Column(name = "anio", nullable = false)
    private short anio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVehiculo estado;

    @Column(name = "sucursal", nullable = false, length = 40)
    private String sucursal;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    /** Constructor vacío que exige JPA; no se usa desde el código. */
    protected Vehiculo() {
    }

    public String getPlaca() { return placa; }
    public String getCategoria() { return categoria; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public short getAnio() { return anio; }
    public EstadoVehiculo getEstado() { return estado; }
    public String getSucursal() { return sucursal; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }

    /** Cambia el estado y actualiza la fecha de modificación. Las reglas de negocio se validan en FlotaService. */
    public void cambiarEstado(EstadoVehiculo nuevoEstado) {
        this.estado = nuevoEstado;
        this.actualizadoEn = Instant.now();
    }
}
