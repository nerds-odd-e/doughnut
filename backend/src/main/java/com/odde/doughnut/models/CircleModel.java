package com.odde.doughnut.models;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;

public class CircleModel {
  @Getter protected final Circle entity;
  protected final ModelFactoryService modelFactoryService;

  public CircleModel(Circle entity, ModelFactoryService modelFactoryService) {
    this.entity = entity;
    this.modelFactoryService = modelFactoryService;
  }

  public void joinAndSave(User user) {
    entity.getMembers().add(user);
    if (entity.getId() == null) {
      modelFactoryService.createRecord(entity);
    } else {
      modelFactoryService.updateRecord(entity);
    }
  }
}
