package com.odde.doughnut.models;

import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;

public abstract class ModelForEntity<T> {
    @Getter
    protected final T entity;
    protected final ModelFactoryService modelFactoryService;

    public ModelForEntity(T entity, ModelFactoryService modelFactoryService) {
        this.entity = entity;
        this.modelFactoryService = modelFactoryService;
    }

    protected void save() {
        modelFactoryService.entityManager.persist(entity);
    }
}
