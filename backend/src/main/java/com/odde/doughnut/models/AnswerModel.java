package com.odde.doughnut.models;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;

public class AnswerModel {
    protected final Answer entity;
    private Boolean cachedIsCorrect;

    public AnswerModel(Answer answer) {
        this.entity = answer;
    }

    public boolean isCorrect() {
        if (cachedIsCorrect == null) {
            cachedIsCorrect = checkResult();
        }
        return cachedIsCorrect;
    }

    private boolean checkResult() {
        return entity.checkAnswer();
    }

    public String answer() {
        return entity.getAnswer();
    }
}
