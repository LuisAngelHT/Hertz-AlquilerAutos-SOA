package com.hertz.incidencia.repository;

import com.hertz.incidencia.entity.FichaDanoInterno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FichaDanoInternoRepository extends JpaRepository<FichaDanoInterno, UUID> {

    List<FichaDanoInterno> findByIdIncidenciaOrderByOrden(UUID idIncidencia);

    void deleteByIdIncidencia(UUID idIncidencia);
}
