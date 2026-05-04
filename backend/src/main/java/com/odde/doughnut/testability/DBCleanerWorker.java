package com.odde.doughnut.testability;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

public class DBCleanerWorker {

  /**
   * Truncate all JPA-mapped tables; use the caller’s {@link EntityManager} (same DB session as
   * seeds).
   */
  public void truncateAllTables(EntityManager entityManager) {
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
    getAnnotatedTableNames(entityManager).forEach(t -> truncateTable(t, entityManager));
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
  }

  private void truncateTable(String tableName, EntityManager entityManager) {
    entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
  }

  private List<String> getAnnotatedTableNames(EntityManager manager) {
    JpaMetamodel metamodel = (JpaMetamodel) manager.getMetamodel();
    Set<EntityType<?>> entities = metamodel.getEntities();

    return entities.stream()
        .map(e -> e.getJavaType().getAnnotation(Table.class))
        .filter(Objects::nonNull)
        .map(Table::name)
        .distinct()
        .sorted()
        .toList();
  }
}
