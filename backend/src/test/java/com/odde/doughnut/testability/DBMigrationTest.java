package com.odde.doughnut;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.Model;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest
class DBMigrationTest {
	@Autowired
	private Flyway flyway;

	@Test
	void migrate() {
		flyway.migrate();
	}

}
