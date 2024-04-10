package com.odde.doughnut;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class DoughnutApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext run = SpringApplication.run(DoughnutApplication.class, args);
    noneApplicationTasks(run);
  }

  private static void noneApplicationTasks(ConfigurableApplicationContext run) {
    if ("migrateTestDB".equals(System.getProperty("odd-e.doughnut.task"))) {
      migrateTestDB(run);
    }
    if ("generateOpenAPIDocs".equals(System.getProperty("odd-e.doughnut.task"))) {
      generateOpenAPIDocs(run);
    }
  }

  private static void migrateTestDB(ConfigurableApplicationContext run) {
    try (run) {
      Flyway flyway = run.getBean(Flyway.class);
      flyway.repair();
      flyway.migrate();
      SpringApplication.exit(run, () -> 0);
    }
  }

  private static void generateOpenAPIDocs(ConfigurableApplicationContext context) {
    int port = getPort(context);
    String docsUrl = "http://localhost:" + port + "/api-docs.yaml";

    RestTemplate restTemplate = new RestTemplate();
    try {
      String openApiDocs = restTemplate.getForObject(docsUrl, String.class);

      // Save the OpenAPI docs to a file
      Files.writeString(Paths.get("../open_api_docs.yaml"), openApiDocs);
      System.out.println("OpenAPI documentation saved successfully.");
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Properly shutdown the application after saving the documentation
    SpringApplication.exit(context, () -> 0);
  }

  private static int getPort(ConfigurableApplicationContext context) {
    if (context instanceof ServletWebServerApplicationContext) {
      ServletWebServerApplicationContext serverContext =
          (ServletWebServerApplicationContext) context;
      return serverContext.getWebServer().getPort();
    }
    System.out.println("Application is not running on a web server.");
    System.exit(1);
    throw new RuntimeException("Application is not running on a web server.");
  }
}
