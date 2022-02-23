package com.odde.doughnut.models;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.sql.Timestamp;

public class CommentModel {
    protected final Comment entity;
    protected final ModelFactoryService modelFactoryService;

    public CommentModel(Comment comment, ModelFactoryService modelFactoryService) {
        this.entity = comment;
        this.modelFactoryService = modelFactoryService;
    }

    public void destroy(Timestamp currentUTCTimestamp) {
        modelFactoryService.commentRepository.softDelete(entity, currentUTCTimestamp);
    }
}
