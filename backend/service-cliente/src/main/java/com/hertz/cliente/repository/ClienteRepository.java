package com.hertz.cliente.repository;

import com.hertz.cliente.entity.Cliente;
import com.hertz.cliente.entity.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
    boolean existsByEmailIgnoreCase(String email);

    Optional<Cliente> findByEmailIgnoreCase(String email);

    /** Listado paginado; "texto" (en minúsculas, con % a los lados; "%" = todos) busca en nombre, documento y correo. */
    @Query("""
            select c from Cliente c
            where lower(concat(c.nombres, ' ', c.apellidos, ' ', c.numeroDocumento, ' ', c.email)) like :texto
            """)
    Page<Cliente> buscar(@Param("texto") String texto, Pageable pageable);
}