package com.hertz.incidencia.repository;

import com.hertz.incidencia.entity.FichaTaller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FichaTallerRepository extends JpaRepository<FichaTaller, UUID> {
}
