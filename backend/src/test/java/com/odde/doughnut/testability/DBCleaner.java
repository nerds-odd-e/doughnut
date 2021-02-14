package com.odde.doughnut.testability;

import org.hibernate.Metamodel;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DBCleaner implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    truncateAllTables();
  }

  @Transactional
  private void truncateAllTables() {
    withEntityManager(entityManager
                      -> guessTableNames(entityManager)
                             .forEach(t -> truncateTable(t, entityManager)));
  }

  private EntityManager createEntityManager() {
    ApplicationContext context =
        ApplicationContextForRepositoriesHolder.getApplicationContext();
    EntityManagerFactory emf =
        context.getBean("emf", EntityManagerFactory.class);
    return emf.createEntityManager();
  }

  private void withEntityManager(Consumer<EntityManager> consumer) {
    EntityManager manager = createEntityManager();
    EntityTransaction transaction = manager.getTransaction();
    transaction.begin();
    consumer.accept(manager);
    transaction.commit();
    manager.close();
  }

  private void truncateTable(String tableName, EntityManager entityManager) {
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
    entityManager.createNativeQuery("ALTER TABLE `" + tableName + "` AUTO_INCREMENT=1").executeUpdate();
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
  }

  private List<String> guessTableNames(EntityManager manager) {
    Metamodel metamodel = (Metamodel)manager.getMetamodel();
    Set<EntityType<?>> entities = metamodel.getEntities();

    return entities.stream()
        .map(EntityType::getName)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
  }
}
