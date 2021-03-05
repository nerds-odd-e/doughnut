package com.odde.doughnut.testability;

import com.odde.doughnut.models.ModelForEntity;

public class EntityAndModelBuilder<T, M extends ModelForEntity<T>> {
    protected final MakeMe makeMe;
    protected final T entity;

    public EntityAndModelBuilder(MakeMe makeMe, T reviewPointEntity) {
        this.makeMe = makeMe;
        entity = reviewPointEntity;
    }
}
