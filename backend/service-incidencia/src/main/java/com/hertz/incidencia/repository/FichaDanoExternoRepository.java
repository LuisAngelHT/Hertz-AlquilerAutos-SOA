package com.hertz.incidencia.repository;

import com.hertz.incidencia.entity.FichaDanoExterno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FichaDanoExternoRepository extends JpaRepository<FichaDanoExterno, UUID> {

    List<FichaDanoExterno> findByIdIncidenciaOrderByOrden(UUID idIncidencia);

    void deleteByIdIncidencia(UUID idIncidencia);
}
