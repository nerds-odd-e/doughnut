package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class LinkModel {
  protected final Link entity;
  protected final ModelFactoryService modelFactoryService;

  public LinkModel(Link link, ModelFactoryService modelFactoryService) {
    this.entity = link;
    this.modelFactoryService = modelFactoryService;
  }

  public void destroy() {
    modelFactoryService.remove(entity);
    modelFactoryService.entityManager.flush();
  }
}
