package com.odde.donut.factoryServices;

import com.odde.donut.entities.EntityIdentifiedByIdOnly;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.function.Supplier;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntityPersister {
  private final EntityManager entityManager;

  public EntityPersister(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Transactional
  public <T> T save(T entity) {
    if (entity instanceof EntityIdentifiedByIdOnly instance) {
      if (instance.getId() != null) {
        return entityManager.merge(entity);
      }
    }
    entityManager.persist(entity);
    return entity;
  }

  @Transactional
  public <T extends EntityIdentifiedByIdOnly> T merge(T entity) {
    return entityManager.merge(entity);
  }

  @Transactional
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

  public void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  /**
   * Runs a pure read without flushing before each query, which otherwise rescans every loaded
   * entity and makes large reads quadratic. Pending changes are flushed first so queries see them.
   */
  public <T> T readWithoutAutoFlush(Supplier<T> read) {
    Session session = entityManager.unwrap(Session.class);
    session.flush();
    FlushMode previous = session.getHibernateFlushMode();
    session.setHibernateFlushMode(FlushMode.MANUAL);
    try {
      return read.get();
    } finally {
      session.setHibernateFlushMode(previous);
    }
  }

  public void refresh(Object entity) {
    entityManager.refresh(entity);
  }

  public void detach(Object entity) {
    entityManager.detach(entity);
  }

  public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
    return entityManager.createQuery(qlString, resultClass);
  }
}
