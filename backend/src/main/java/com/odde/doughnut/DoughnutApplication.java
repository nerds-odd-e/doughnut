package com.odde.doughnut;

import com.odde.doughnut.configs.DoughnutTaskRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DoughnutApplication {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DoughnutApplication.class);

  public static void main(String[] args) {
    logger.info("Starting DoughnutApplication...");
    ConfigurableApplicationContext run = SpringApplication.run(DoughnutApplication.class, args);
    logger.info("DoughnutApplication started successfully");
    noneApplicationTasks(run);
  }

  private static void noneApplicationTasks(ConfigurableApplicationContext run) {
    DoughnutTaskRunner taskRunner = new DoughnutTaskRunner(run);
    if ("migrateTestDB".equals(System.getProperty("odd-e.doughnut.task"))) {
      taskRunner.migrateTestDB();
    }
    if ("generateOpenAPIDocs".equals(System.getProperty("odd-e.doughnut.task"))) {
      taskRunner.generateOpenAPIDocs();
    }
  }
}
