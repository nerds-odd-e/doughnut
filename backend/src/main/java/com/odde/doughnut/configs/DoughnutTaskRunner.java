package com.odde.doughnut.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestTemplate;

public record DoughnutTaskRunner(ConfigurableApplicationContext context) {
  private int getPort() {
    if (context instanceof ServletWebServerApplicationContext serverContext) {
      return serverContext.getWebServer().getPort();
    }
    System.out.println("Application is not running on a web server.");
    System.exit(-1);
    throw new RuntimeException("Application is not running on a web server.");
  }

  private void runTask(Runnable task) {
    try {
      task.run();
      SpringApplication.exit(context, () -> 0);
    } catch (Exception e) {
      e.printStackTrace();
      SpringApplication.exit(context, () -> -1);
    }
  }

  public void generateOpenAPIDocs() {
    runTask(
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
          System.out.println("OpenAPI documentation saved successfully.");
        });
  }

  public void migrateTestDB() {
    runTask(
        () -> {
          Flyway flyway = context.getBean(Flyway.class);
          flyway.repair();
          flyway.migrate();
          System.out.println("Test database migrated successfully.");
        });
  }
}
