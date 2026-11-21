package com.hertz.contrato.repository;

import com.hertz.contrato.dto.DocumentoMeta;
import com.hertz.contrato.entity.ContratoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContratoDocumentoRepository extends JpaRepository<ContratoDocumento, UUID> {

    /** Solo los datos (nunca el archivo): sirve para marcar en el listado que contratos ya tienen su escaneo. */
    @Query("""
            select new com.hertz.contrato.dto.DocumentoMeta(
                d.idContrato, d.nombreArchivo, d.tipoContenido, d.tamanoBytes, d.subidoPor, d.subidoEn)
            from ContratoDocumento d
            where d.idContrato in :ids
            """)
    List<DocumentoMeta> metasPorContratos(@Param("ids") Collection<UUID> ids);
}
