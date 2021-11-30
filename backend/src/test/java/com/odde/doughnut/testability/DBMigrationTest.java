package com.odde.doughnut.testability;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DBMigrationTest {
  @Autowired private Flyway flyway;

  @Test
  @Tag("dbMigrate")
  void migrate() {
    flyway.repair();
    flyway.migrate();
  }
}
