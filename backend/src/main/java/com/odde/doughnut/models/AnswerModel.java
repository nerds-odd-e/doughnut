package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.services.ModelFactoryService;

public class AnswerModel extends ModelForEntity<AnswerEntity> {
    private Boolean cachedIsCorrect;

    public AnswerModel(AnswerEntity answerEntity, ModelFactoryService modelFactoryService) {
        super(answerEntity, modelFactoryService);
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
