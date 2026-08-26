package com.odde.donut;

import com.odde.donut.configs.DonutTaskRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DonutApplication {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DonutApplication.class);

  public static void main(String[] args) {
    logger.info("Starting DonutApplication...");
    ConfigurableApplicationContext run = SpringApplication.run(DonutApplication.class, args);
    logger.info("DonutApplication started successfully");
    noneApplicationTasks(run);
  }

  private static void noneApplicationTasks(ConfigurableApplicationContext run) {
    DonutTaskRunner taskRunner = new DonutTaskRunner(run);
    if ("migrateTestDB".equals(System.getProperty("odd-e.donut.task"))) {
      taskRunner.migrateTestDB();
    }
    if ("generateOpenAPIDocs".equals(System.getProperty("odd-e.donut.task"))) {
      taskRunner.generateOpenAPIDocs();
    }
  }
}
