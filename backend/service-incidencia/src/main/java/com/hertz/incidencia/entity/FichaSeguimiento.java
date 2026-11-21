package com.hertz.incidencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Una nota de avance del taller, con quien la escribio y cuando. */
@Entity
@Table(name = "ficha_seguimiento", schema = "incidencia")
public class FichaSeguimiento {

    @Id
    private UUID id;

    @Column(name = "id_incidencia", nullable = false)
    private UUID idIncidencia;

    @Column(nullable = false, length = 500)
    private String nota;

    @Column(name = "registrado_por", nullable = false, length = 120)
    private String registradoPor;

    @Column(name = "registrado_en", nullable = false)
    private Instant registradoEn;

    protected FichaSeguimiento() {
    }

    public FichaSeguimiento(UUID idIncidencia, String nota, String registradoPor) {
        this.id = UUID.randomUUID();
        this.idIncidencia = idIncidencia;
        this.nota = nota;
        this.registradoPor = registradoPor;
        this.registradoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getIdIncidencia() { return idIncidencia; }
    public String getNota() { return nota; }
    public String getRegistradoPor() { return registradoPor; }
    public Instant getRegistradoEn() { return registradoEn; }
}
