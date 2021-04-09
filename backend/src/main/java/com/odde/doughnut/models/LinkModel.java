package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.services.ModelFactoryService;

public class LinkModel extends ModelForEntity<Link>{
    public LinkModel(Link link, ModelFactoryService modelFactoryService) {
        super(link, modelFactoryService);
    }

    public void destroy() {
        modelFactoryService.linkRepository.delete(entity);
    }
}
