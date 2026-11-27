package com.udea.parcial_equipo_4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto arranca con H2 (modo PostgreSQL), aplicando las
 * migraciones de Flyway y validando las entidades JPA contra ese esquema.
 * La configuración está en src/test/resources/application.properties.
 */
@SpringBootTest
class ParcialEquipo4ApplicationTests {

	@Test
	void contextLoads() {
	}

}
