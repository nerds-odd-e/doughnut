package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.Optional;

public class QuizQuestion {
    private final ModelFactoryService modelFactoryService;

    public QuizQuestion(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }
}
