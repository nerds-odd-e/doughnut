package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class AnswerModel {
  public final Answer answer;
  private final ModelFactoryService modelFactoryService;

  public AnswerModel(Answer answer, ModelFactoryService modelFactoryService) {
    this.answer = answer;
    this.modelFactoryService = modelFactoryService;
  }

  public void save() {
    modelFactoryService.answerRepository.save(answer);
  }

  public AnsweredQuestion getAnswerViewedByUser(User user) {
    return answer.getViewedByUser(user, modelFactoryService);
  }
}
