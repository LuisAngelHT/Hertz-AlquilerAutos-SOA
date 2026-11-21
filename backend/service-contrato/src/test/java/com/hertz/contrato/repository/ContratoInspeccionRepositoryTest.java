package com.hertz.contrato.repository;

import com.hertz.contrato.dto.InspeccionResponse;
import com.hertz.contrato.entity.Combustible;
import com.hertz.contrato.entity.Contrato;
import com.hertz.contrato.entity.ContratoDano;
import com.hertz.contrato.entity.ContratoInspeccion;
import com.hertz.contrato.entity.ContratoItem;
import com.hertz.contrato.entity.EstadoItem;
import com.hertz.contrato.entity.GravedadDano;
import com.hertz.contrato.entity.ItemInventario;
import com.hertz.contrato.entity.TipoDano;
import com.hertz.contrato.entity.TipoInspeccion;
import com.hertz.contrato.entity.VistaVehiculo;
import com.hertz.contrato.entity.ZonaVehiculo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prueba con base de datos real (H2): al traer una revision con sus danos Y su inventario, los danos no deben salir
 * repetidos (la base devuelve una fila por cada combinacion dano x elemento).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contrato_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS contrato",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "gateway.secret=test-only-gateway-secret"
})
class ContratoInspeccionRepositoryTest {

    @Autowired
    private ContratoRepository contratos;

    @Autowired
    private ContratoInspeccionRepository inspecciones;

    @Autowired
    private TransactionTemplate transacciones;

    @Test
    void losDanosNoSeRepitenAlTraerlosJuntoConElInventario() {
        Contrato contrato = new Contrato(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ABC-103",
                "LIMA_AEROPUERTO", "LIMA_AEROPUERTO", LocalDate.now(), LocalDate.now().plusDays(1), "agente@hertz.test");
        List<ContratoDano> danos = List.of(
                new ContratoDano(ZonaVehiculo.FRONTAL, "Parachoques hundido", GravedadDano.GRAVE, new BigDecimal("420.00"),
                        VistaVehiculo.FRENTE, new BigDecimal("50.00"), new BigDecimal("70.00"), TipoDano.ABOLLADURA),
                new ContratoDano(ZonaVehiculo.LATERAL_DERECHO, "Puerta abollada", GravedadDano.MODERADO, new BigDecimal("150.00"),
                        VistaVehiculo.DERECHA, new BigDecimal("55.00"), new BigDecimal("52.00"), TipoDano.ABOLLADURA),
                new ContratoDano(ZonaVehiculo.INTERIOR, "Quemadura en el asiento", GravedadDano.LEVE, new BigDecimal("60.00")));
        List<ContratoItem> items = java.util.Arrays.stream(ItemInventario.values())
                .map(i -> new ContratoItem(i, i == ItemInventario.EXTINTOR ? EstadoItem.FALTA : EstadoItem.OK,
                        i == ItemInventario.EXTINTOR ? new BigDecimal("90.00") : null))
                .toList();   // 24 elementos
        ContratoInspeccion revision = new ContratoInspeccion(contrato.getId(), TipoInspeccion.DEVOLUCION, 30410,
                Combustible.MEDIO, null, true, "agente@hertz.test", danos, items);

        transacciones.executeWithoutResult(estado -> {
            contratos.saveAndFlush(contrato);
            inspecciones.saveAndFlush(revision);
        });

        // Se lee fuera de la transaccion, como lo hace el servicio al armar la respuesta.
        List<ContratoInspeccion> leidas = inspecciones.findByIdContratoIn(List.of(contrato.getId()));
        InspeccionResponse respuesta = InspeccionResponse.de(leidas.get(0));

        assertEquals(1, leidas.size());
        assertEquals(3, respuesta.danos().size());              // sin repetidos
        assertEquals(24, respuesta.inventario().size());
        assertEquals(1, respuesta.danos().stream().filter(d -> "FRENTE".equals(d.vista())).count());
        assertEquals(new BigDecimal("420.00"), respuesta.danos().stream()
                .filter(d -> "FRENTE".equals(d.vista())).findFirst().orElseThrow().costoEstimado());
    }
}
