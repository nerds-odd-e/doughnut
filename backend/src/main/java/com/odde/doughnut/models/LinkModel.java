package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;

public class LinkModel {
    protected final Link entity;
    protected final ModelFactoryService modelFactoryService;

    public LinkModel(Link link, ModelFactoryService modelFactoryService) {
        this.entity = link;
        this.modelFactoryService = modelFactoryService;
    }

    public void destroy() {
        modelFactoryService.linkRepository.delete(entity);
    }

}
