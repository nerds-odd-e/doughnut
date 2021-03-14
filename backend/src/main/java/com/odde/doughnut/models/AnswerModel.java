package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.services.ModelFactoryService;

public class AnswerModel extends ModelForEntity<AnswerEntity>{
    private Boolean cachedIsCorrect;

    public AnswerModel(AnswerEntity answerEntity, ModelFactoryService modelFactoryService) {
        super(answerEntity, modelFactoryService);
    }

    public void checkResult() {
        cachedIsCorrect = (
                entity.getAnswer().toLowerCase().trim().equals(
                        entity.getReviewPointEntity().getNoteEntity().getTitle().toLowerCase().trim()));
    }

    public boolean isCorrect() {
        if (cachedIsCorrect == null) checkResult();
        return cachedIsCorrect;
    }

    public String answer() {
        return entity.getAnswer();
    }
}
