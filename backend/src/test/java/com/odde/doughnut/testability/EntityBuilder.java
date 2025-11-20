package com.odde.doughnut.testability;

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
      makeMe.entityPersister.save(entity);
    }
    afterCreate(persistNeeded);
    return entity;
  }

  protected abstract void beforeCreate(boolean needPersist);

  protected void afterCreate(boolean needPersist) {}
  ;
}
