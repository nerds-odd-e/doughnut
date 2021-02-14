package com.odde.doughnut.testability;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;

import javax.persistence.EntityManagerFactory;

public class DBCleaner implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext ctx) {
    ApplicationContext context =
            ApplicationContextForRepositoriesHolder.getApplicationContext();
    EntityManagerFactory emf =
            context.getBean("emf", EntityManagerFactory.class);
    new DBCleanerWorker(emf).truncateAllTables();
  }
}
