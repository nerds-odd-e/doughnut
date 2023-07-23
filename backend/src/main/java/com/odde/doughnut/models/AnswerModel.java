package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class AnswerModel {
  public final Answer answer;
  private final ModelFactoryService modelFactoryService;

  public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
    this.answer = answer;
    this.modelFactoryService = modelFactoryService;
  }

  public void updateReviewPoints(Timestamp currentUTCTimestamp, boolean correct) {
    answer
        .getQuestion()
        .getRelatedReviewPoints()
        .map(this.modelFactoryService::toReviewPointModel)
        .forEach(model -> model.updateAfterRepetition(currentUTCTimestamp, correct));
  }

  public void save() {
    modelFactoryService.answerRepository.save(answer);
  }

  public AnsweredQuestion getAnswerViewedByUser(User user) {
    return answer.getViewedByUser(user, modelFactoryService);
  }
}
