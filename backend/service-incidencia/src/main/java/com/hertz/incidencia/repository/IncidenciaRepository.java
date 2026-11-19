package com.hertz.incidencia.repository;

import com.hertz.incidencia.entity.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface IncidenciaRepository
        extends JpaRepository<Incidencia, UUID>, JpaSpecificationExecutor<Incidencia> {
}