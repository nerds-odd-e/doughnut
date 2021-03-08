package com.odde.doughnut.models;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.services.ModelFactoryService;

public class CircleModel extends ModelForEntity<CircleEntity> {
    public CircleModel(CircleEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }
}
