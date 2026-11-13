package com.hertz.cliente.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cliente", schema = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "licencia_conducir", length = 20)
    private String licenciaConducir;

    @Column(name = "vencimiento_licencia")
    private LocalDate vencimientoLicencia;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", insertable = false)
    private Instant actualizadoEn;

    protected Cliente() {
    }

    public Cliente(TipoDocumento tipoDocumento, String numeroDocumento, String nombres, String apellidos,
                   LocalDate fechaNacimiento, String email, String telefono,
                   String licenciaConducir, LocalDate vencimientoLicencia) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.email = email;
        this.telefono = telefono;
        this.licenciaConducir = licenciaConducir;
        this.vencimientoLicencia = vencimientoLicencia;
    }

    public void actualizarDatos(String nombres, String apellidos, String email, String telefono,
                                String licenciaConducir, LocalDate vencimientoLicencia) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.licenciaConducir = licenciaConducir;
        this.vencimientoLicencia = vencimientoLicencia;
    }

    @PreUpdate
    void alActualizar() {
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getLicenciaConducir() { return licenciaConducir; }
    public LocalDate getVencimientoLicencia() { return vencimientoLicencia; }
}