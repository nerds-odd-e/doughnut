package com.odde.doughnut.testability;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
class DBMigrationTest {
	@Autowired
	private Flyway flyway;

	@Test
	@Tag("dbMigrate")
	void migrate() {
		flyway.migrate();
	}

}
