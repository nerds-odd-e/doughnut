package com.odde.doughnut.testability;

public abstract class EntityBuilder<T> {
    protected final MakeMe makeMe;
    protected final T entity;

    public EntityBuilder(MakeMe makeMe, T entity) {
        this.makeMe = makeMe;
        this.entity = entity;
    }

    public T inMemoryPlease() {
        return entity;
    }

    public T please() {
        beforeCreate();
        makeMe.modelFactoryService.entityManager.persist(entity);
        return entity;
    }

    protected abstract void beforeCreate();

}
