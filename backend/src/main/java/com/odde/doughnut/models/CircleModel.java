package com.odde.doughnut.models;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class CircleModel extends ModelForEntity<Circle> {
    public CircleModel(Circle entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void joinAndSave(UserModel userModel) {
        entity.getMembers().add(userModel.getEntity());
        modelFactoryService.circleRepository.save(entity);
    }

}
