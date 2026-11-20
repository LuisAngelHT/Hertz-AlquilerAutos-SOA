package com.hertz.incidencia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Base H2 en memoria y secreto de prueba: así el contexto carga sin Supabase ni variables de entorno.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:incidencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS incidencia",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "gateway.secret=test-only-gateway-secret"
})
class ServiceIncidenciaApplicationTests {

	@Test
	void contextLoads() {
	}

}
