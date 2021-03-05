package com.odde.doughnut.testability;

import com.odde.doughnut.models.ModelForEntity;
import com.odde.doughnut.models.ReviewPointModel;

import java.util.function.Supplier;

public abstract class EntityAndModelBuilder<T, M extends ModelForEntity<T>> {
    protected final MakeMe makeMe;
    protected final T entity;

    public ReviewPointModel toModelPlease() {
        return makeMe.toModel(please());
    }

    public EntityAndModelBuilder(MakeMe makeMe, T reviewPointEntity) {
        this.makeMe = makeMe;
        entity = reviewPointEntity;
    }

    public T please() {
        makeMe.modelFactoryService.entityManager.persist(entity);
        return entity;
    }
}
