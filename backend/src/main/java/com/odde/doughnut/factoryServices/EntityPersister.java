package com.odde.doughnut.factoryServices;

import com.odde.doughnut.entities.EntityIdentifiedByIdOnly;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityPersister {
  @Autowired private EntityManager entityManager;

  public <T> T save(T entity) {
    if (entity instanceof EntityIdentifiedByIdOnly instance) {
      if (instance.getId() != null) {
        return entityManager.merge(entity);
      }
    }
    entityManager.persist(entity);
    return entity;
  }

  public <T extends EntityIdentifiedByIdOnly> T merge(T entity) {
    return entityManager.merge(entity);
  }

  public <T extends EntityIdentifiedByIdOnly> T remove(T entity) {
    T merged = entityManager.merge(entity);
    entityManager.remove(merged);
    entityManager.flush();
    return merged;
  }

  public <T> T find(Class<T> entityClass, Object primaryKey) {
    return entityManager.find(entityClass, primaryKey);
  }

  public void flush() {
    entityManager.flush();
  }

  public void refresh(Object entity) {
    entityManager.refresh(entity);
  }
}
