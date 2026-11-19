package com.hertz.contrato.repository;

import com.hertz.contrato.entity.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ContratoRepository
        extends JpaRepository<Contrato, UUID>, JpaSpecificationExecutor<Contrato> {

    Optional<Contrato> findByIdReserva(UUID idReserva);
}
