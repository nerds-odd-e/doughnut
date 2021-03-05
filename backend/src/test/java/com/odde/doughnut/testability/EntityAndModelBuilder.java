package com.odde.doughnut.testability;

import com.odde.doughnut.models.ModelForEntity;

public abstract class EntityAndModelBuilder<T, M extends ModelForEntity<T>> extends EntityBuilder<T> {
    protected final Class<M> mClass;

    public EntityAndModelBuilder(MakeMe makeMe, T entity, Class<M> mClass) {
        super(makeMe, entity);
        this.mClass = mClass;
    }

    public M toModelPlease() {
        return makeMe.modelFactoryService.toModel(please(), mClass);
    }

}
