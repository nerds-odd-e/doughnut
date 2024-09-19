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

  public void save() {
    modelFactoryService.save(answer);
  }

  public AnsweredQuestion getAnswerViewedByUser(User user) {
    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer;
    answerResult.reviewPoint = getReviewPoint(user);
    answerResult.predefinedQuestion = getQuestion();
    String result;
    if (answer.getChoiceIndex() != null) {
      result =
          getQuestion()
              .getBareQuestion()
              .getMultipleChoicesQuestion()
              .getChoices()
              .get(answer.getChoiceIndex());
    } else {
      result = answer.getSpellingAnswer();
    }
    answerResult.answerDisplay = result;
    return answerResult;
  }

  private PredefinedQuestion getQuestion() {
    return answer.getReviewQuestionInstance().getPredefinedQuestion();
  }

  public void makeAnswerToQuestion(Timestamp currentUTCTimestamp, User user) {
    save();
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
