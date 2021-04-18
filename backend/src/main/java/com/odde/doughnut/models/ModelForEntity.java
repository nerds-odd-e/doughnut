package com.odde.doughnut.models;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;

public abstract class ModelForEntity<T> {
    @Getter
    protected final T entity;
    protected final ModelFactoryService modelFactoryService;

    public ModelForEntity(T entity, ModelFactoryService modelFactoryService) {
        this.entity = entity;
        this.modelFactoryService = modelFactoryService;
    }

    public void save() {
        modelFactoryService.entityManager.persist(entity);
    }
}
