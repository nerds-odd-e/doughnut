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

  private PredefinedQuestion getQuestion() {
    return answer.getReviewQuestionInstance().getPredefinedQuestion();
  }

  public void makeAnswerToQuestion(Timestamp currentUTCTimestamp, User user) {
    modelFactoryService.save(answer);
    modelFactoryService.save(answer.getReviewQuestionInstance());
    ReviewPoint reviewPoint = getReviewPoint(user);
    if (reviewPoint == null) return;
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .markAsRepeated(currentUTCTimestamp, answer.getCorrect());
  }

  private ReviewPoint getReviewPoint(User user) {
    return modelFactoryService.toUserModel(user).getReviewPointFor(getQuestion().getNote());
  }
}
