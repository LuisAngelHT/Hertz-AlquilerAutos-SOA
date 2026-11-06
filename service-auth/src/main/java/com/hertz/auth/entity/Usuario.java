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
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    protected Usuario() {
    }

    public UUID getId() { return id; }
    public String getCorreo() { return correo; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getUltimoLogin() { return ultimoLogin; }

    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }
}