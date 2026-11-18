package com.hertz.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario", schema = "autenticacion")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "cliente_id")
    private UUID clienteId;

    // Sede del AGENTE o ADMINISTRADOR_SUCURSAL (ej. LIMA_AEROPUERTO). Es null para el
    // ADMINISTRADOR general y para los clientes.
    @Column(name = "sucursal", length = 40)
    private String sucursal;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    // Control de intentos de login: tras varios fallos seguidos la cuenta se bloquea un rato.
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected Usuario() {
    }

    public Usuario(String correo, String nombre, String passwordHash, Rol rol, UUID clienteId) {
        this.correo = correo;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.clienteId = clienteId;
        this.activo = true;
    }

    public UUID getId() { return id; }
    public String getCorreo() { return correo; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public UUID getClienteId() { return clienteId; }
    public String getSucursal() { return sucursal; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getUltimoLogin() { return ultimoLogin; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }

    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }

    /** ¿La cuenta sigue bloqueada en este instante? */
    public boolean estaBloqueada(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Suma un intento fallido; al llegar al máximo bloquea la cuenta y reinicia el contador. */
    public void registrarIntentoFallido(int maximo, Instant bloqueadoHasta) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= maximo) {
            this.bloqueadoHasta = bloqueadoHasta;
            this.intentosFallidos = 0;
        }
    }

    /** Un login correcto borra los intentos fallidos y cualquier bloqueo vencido. */
    public void limpiarIntentos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }
}