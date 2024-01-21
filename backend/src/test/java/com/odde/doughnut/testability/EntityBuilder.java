package com.odde.doughnut.testability;

import com.odde.doughnut.entities.EntityIdentifiedByIdOnly;

public abstract class EntityBuilder<T> {
  protected final MakeMe makeMe;
  protected T entity;

  public EntityBuilder(MakeMe makeMe, T entity) {
    this.makeMe = makeMe;
    this.entity = entity;
  }

  public T inMemoryPlease() {
    return please(false);
  }

  public T please() {
    return please(true);
  }

  public T please(boolean persistNeeded) {
    beforeCreate(persistNeeded);
    if (persistNeeded) {
      if (entity instanceof EntityIdentifiedByIdOnly) {
        makeMe.modelFactoryService.save((EntityIdentifiedByIdOnly) entity);
      } else {
        makeMe.modelFactoryService.entityManager.persist(entity);
      }
    }
    return entity;
  }

  protected abstract void beforeCreate(boolean needPersist);
}
