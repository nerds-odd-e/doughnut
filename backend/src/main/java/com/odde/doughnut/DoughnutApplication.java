package com.odde.doughnut;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DoughnutApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext run = SpringApplication.run(DoughnutApplication.class, args);
    migrateTestDb(run);
  }

  private static void migrateTestDb(ConfigurableApplicationContext run) {
    if (!"testDBMigrate".equals(System.getProperty("odd-e.doughnut.task"))) {
      return;
    }
    Flyway flyway = run.getBean(Flyway.class);
    flyway.repair();
    flyway.migrate();
    run.close();
  }
}
