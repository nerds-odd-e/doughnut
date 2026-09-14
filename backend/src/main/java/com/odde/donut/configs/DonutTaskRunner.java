package com.odde.donut.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.flywaydb.core.Flyway;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestTemplate;

public record DonutTaskRunner(ConfigurableApplicationContext context) {
  public static final String PORTABLE_TRASH_UPGRADE_SUCCESS = "PORTABLE_TRASH_UPGRADE_SUCCESS";

  private int getPort() {
    if (context instanceof ServletWebServerApplicationContext serverContext) {
      return serverContext.getWebServer().getPort();
    }
    throw new IllegalStateException("Application is not running on a web server.");
  }

  private int runTask(Runnable task, String successMessage) {
    int exitCode = 0;
    try {
      task.run();
    } catch (Exception e) {
      e.printStackTrace();
      exitCode = -1;
    }
    try {
      context.close();
    } catch (Exception e) {
      e.printStackTrace();
      exitCode = -1;
    }
    if (exitCode == 0) {
      System.out.println(successMessage);
    }
    return exitCode;
  }

  public int generateOpenAPIDocs() {
    return runTask(
        () -> {
          String docsUrl = "http://localhost:" + getPort() + "/api-docs.yaml";
          RestTemplate restTemplate = new RestTemplate();
          String openApiDocs = restTemplate.getForObject(docsUrl, String.class);
          // Save the OpenAPI docs to a file
          try {
            Files.writeString(Paths.get("../open_api_docs.yaml"), openApiDocs);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        },
        "OpenAPI documentation saved successfully.");
  }

  public int migrateTestDB() {
    return runTask(this::migrateDatabase, "Test database migrated successfully.");
  }

  public int upgradePortableTrash() {
    return runTask(this::migrateDatabase, PORTABLE_TRASH_UPGRADE_SUCCESS);
  }

  private void migrateDatabase() {
    Flyway flyway = context.getBean(Flyway.class);
    flyway.repair();
    flyway.migrate();
  }
}
