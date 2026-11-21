package com.hertz.incidencia.repository;

import com.hertz.incidencia.entity.FichaSeguimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FichaSeguimientoRepository extends JpaRepository<FichaSeguimiento, UUID> {

    List<FichaSeguimiento> findByIdIncidenciaOrderByRegistradoEnDesc(UUID idIncidencia);
}
