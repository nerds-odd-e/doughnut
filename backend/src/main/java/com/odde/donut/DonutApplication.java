package com.odde.donut;

import com.odde.donut.configs.DonutTaskRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DonutApplication {

  public static final String PORTABLE_TRASH_UPGRADE_PROFILE = "portable-trash-upgrade";
  static final String PORTABLE_TRASH_UPGRADE_TASK = "upgradePortableTrash";
  private static final String APPLICATION_TASK_PROPERTY = "odd-e.donut.task";

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DonutApplication.class);

  public static void main(String[] args) {
    logger.info("Starting DonutApplication...");
    String task = System.getProperty(APPLICATION_TASK_PROPERTY);
    SpringApplication application = applicationForTask(task);
    ConfigurableApplicationContext context = application.run(args);
    logger.info("DonutApplication started successfully");
    runApplicationTask(context, task);
  }

  static SpringApplication applicationForTask(String task) {
    SpringApplication application = new SpringApplication(DonutApplication.class);
    if (PORTABLE_TRASH_UPGRADE_TASK.equals(task)) {
      application.setWebApplicationType(WebApplicationType.NONE);
      application.setAdditionalProfiles(PORTABLE_TRASH_UPGRADE_PROFILE);
    }
    return application;
  }

  private static void runApplicationTask(ConfigurableApplicationContext context, String task) {
    DonutTaskRunner taskRunner = new DonutTaskRunner(context);
    if ("migrateTestDB".equals(task)) {
      taskRunner.migrateTestDB();
    }
    if ("generateOpenAPIDocs".equals(task)) {
      taskRunner.generateOpenAPIDocs();
    }
  }
}
