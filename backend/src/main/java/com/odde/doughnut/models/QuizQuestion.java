package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.regex.Pattern;

public class QuizQuestion {
    private final NoteEntity noteEntity;
    private final ModelFactoryService modelFactoryService;

    public QuizQuestion(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        this.noteEntity = noteEntity;
        this.modelFactoryService = modelFactoryService;
    }

    public String getDescription() {
        Pattern pattern = Pattern.compile(Pattern.quote(noteEntity.getTitle()), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(noteEntity.getDescription()).replaceAll("[...]");
    }
}
