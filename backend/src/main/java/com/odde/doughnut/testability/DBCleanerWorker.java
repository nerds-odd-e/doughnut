package com.odde.doughnut.testability;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.springframework.transaction.annotation.Transactional;

public class DBCleanerWorker {
  private final EntityManagerFactory emf;

  public DBCleanerWorker(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Transactional
  public void truncateAllTables() {
    withEntityManager(
        entityManager ->
            getAnnotatedTableNames(entityManager).forEach(t -> truncateTable(t, entityManager)));
  }

  private EntityManager createEntityManager() {
    return emf.createEntityManager();
  }

  private void withEntityManager(Consumer<EntityManager> consumer) {
    EntityManager manager = createEntityManager();
    EntityTransaction transaction = manager.getTransaction();
    transaction.begin();

    manager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
    consumer.accept(manager);
    manager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
    transaction.commit();
    manager.close();
  }

  private void truncateTable(String tableName, EntityManager entityManager) {
    entityManager.createNativeQuery("DELETE FROM `" + tableName + "`").executeUpdate();
    entityManager
        .createNativeQuery("ALTER TABLE `" + tableName + "` AUTO_INCREMENT=1")
        .executeUpdate();
  }

  private List<String> getAnnotatedTableNames(EntityManager manager) {
    JpaMetamodel metamodel = (JpaMetamodel) manager.getMetamodel();
    Set<EntityType<?>> entities = metamodel.getEntities();

    return entities.stream()
        .map(e -> e.getJavaType().getAnnotation(Table.class))
        .filter(Objects::nonNull)
        .map(Table::name)
        .collect(Collectors.toList());
  }
}
