package com.odde.doughnut.testability;

public abstract class EntityBuilder<T> {
    protected final MakeMe makeMe;
    protected final T entity;

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
            makeMe.modelFactoryService.entityManager.persist(entity);
        }
        return entity;
    }

    protected abstract void beforeCreate(boolean needPersist);

}
