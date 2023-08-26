package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import javax.persistence.EntityManager;

public class EntityIdResolver implements ObjectIdResolver {

  private EntityManager entityManager;

  public EntityIdResolver(final EntityManager entityManager) {

    this.entityManager = entityManager;
  }

  @Override
  public void bindItem(final ObjectIdGenerator.IdKey id, final Object pojo) {}

  @Override
  public Object resolveId(final ObjectIdGenerator.IdKey id) {
    return this.entityManager.find(id.scope, id.key);
  }

  @Override
  public ObjectIdResolver newForDeserialization(final Object context) {

    return this;
  }

  @Override
  public boolean canUseFor(final ObjectIdResolver resolverType) {

    return false;
  }
}
