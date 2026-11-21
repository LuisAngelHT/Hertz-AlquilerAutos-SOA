package com.hertz.contrato.repository;

import com.hertz.contrato.entity.ContratoInspeccion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContratoInspeccionRepository extends JpaRepository<ContratoInspeccion, UUID> {

    /** Las revisiones de varios contratos a la vez, ya con sus danos (una consulta para toda la pagina). */
    @EntityGraph(attributePaths = {"danos", "items"})
    List<ContratoInspeccion> findByIdContratoIn(Collection<UUID> idsContrato);
}
