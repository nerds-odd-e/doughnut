package com.odde.doughnut.models;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.services.ModelFactoryService;

public class AnswerModel extends ModelForEntity<Answer> {
    private Boolean cachedIsCorrect;

    public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
        super(answer, modelFactoryService);
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
