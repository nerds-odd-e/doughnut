package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.services.ModelFactoryService;

public class LinkModel extends ModelForEntity<LinkEntity>{
    public LinkModel(LinkEntity linkEntity, ModelFactoryService modelFactoryService) {
        super(linkEntity, modelFactoryService);
    }

    public void destroy() {
        modelFactoryService.linkRepository.delete(entity);
    }
}
