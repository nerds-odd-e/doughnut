package com.odde.doughnut.testability;

import com.odde.doughnut.models.ModelForEntity;

public abstract class EntityAndModelBuilder<T, M extends ModelForEntity<T>> {
    protected final MakeMe makeMe;
    protected final T entity;
    protected final Class<M> mClass;

    public M toModelPlease() {
        return makeMe.modelFactoryService.toModel(please(), mClass);
    }

    public EntityAndModelBuilder(MakeMe makeMe, T reviewPointEntity, Class<M> mClass) {
        this.makeMe = makeMe;
        entity = reviewPointEntity;
        this.mClass = mClass;
    }

    public T please() {
        makeMe.modelFactoryService.entityManager.persist(entity);
        return entity;
    }

    public T inMemoryPlease() {
        return entity;
    }
}
