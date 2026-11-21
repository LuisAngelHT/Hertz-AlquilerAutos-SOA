package com.hertz.contrato.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Contrato firmado y escaneado. Hay como maximo uno por contrato; reemplazarlo sobrescribe el anterior. */
@Entity
@Table(name = "contrato_documento", schema = "contrato")
public class ContratoDocumento {

    @Id
    @Column(name = "id_contrato")
    private UUID idContrato;

    @Column(name = "nombre_archivo", nullable = false, length = 200)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 30)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private int tamanoBytes;

    @Column(nullable = false)
    private byte[] contenido;

    @Column(name = "subido_por", nullable = false, length = 120)
    private String subidoPor;

    @Column(name = "subido_en", nullable = false)
    private Instant subidoEn;

    protected ContratoDocumento() {
    }

    public ContratoDocumento(UUID idContrato, String nombreArchivo, String tipoContenido,
                             byte[] contenido, String subidoPor) {
        this.idContrato = idContrato;
        reemplazar(nombreArchivo, tipoContenido, contenido, subidoPor);
    }

    public void reemplazar(String nombreArchivo, String tipoContenido, byte[] contenido, String subidoPor) {
        this.nombreArchivo = nombreArchivo;
        this.tipoContenido = tipoContenido;
        this.contenido = contenido;
        this.tamanoBytes = contenido.length;
        this.subidoPor = subidoPor;
        this.subidoEn = Instant.now();
    }

    public UUID getIdContrato() { return idContrato; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getTipoContenido() { return tipoContenido; }
    public int getTamanoBytes() { return tamanoBytes; }
    public byte[] getContenido() { return contenido; }
    public String getSubidoPor() { return subidoPor; }
    public Instant getSubidoEn() { return subidoEn; }
}
