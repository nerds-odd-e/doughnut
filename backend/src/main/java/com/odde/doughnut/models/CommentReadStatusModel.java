package com.odde.doughnut.models;

import com.odde.doughnut.entities.CommentReadStatus;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;

import java.sql.Timestamp;

public class CommentReadStatusModel {

    protected final CommentReadStatus entity;
    protected final ModelFactoryService modelFactoryService;

    public CommentReadStatusModel(CommentReadStatus commentReadStatus, ModelFactoryService modelFactoryService) {
        this.entity = commentReadStatus;
        this.modelFactoryService = modelFactoryService;
    }
}

