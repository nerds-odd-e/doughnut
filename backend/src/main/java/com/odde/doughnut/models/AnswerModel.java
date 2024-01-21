package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import org.jetbrains.annotations.Nullable;

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
    answerResult.answerId = answer.getId();
    answerResult.answerDisplay = answer.getAnswerDisplay(modelFactoryService);
    answerResult.reviewPoint = getReviewPoint(user);
    answerResult.correct = answer.isCorrect();
    answerResult.correctChoiceIndex = getQuestion().getCorrectAnswerIndex();
    answerResult.choiceIndex = answer.getChoiceIndex();
    answerResult.quizQuestion = modelFactoryService.toQuizQuestion(getQuestion(), user);
    return answerResult;
  }

  @Nullable
  private QuizQuestionEntity getQuestion() {
    return answer.getQuestion();
  }

  public void makeAnswerToQuestion(Timestamp currentUTCTimestamp, User user) {
    save();
    ReviewPoint reviewPoint = getReviewPoint(user);
    if (reviewPoint == null) return;
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .markAsRepeated(currentUTCTimestamp, answer.isCorrect());
  }

  private ReviewPoint getReviewPoint(User user) {
    return getQuestion().getReviewPointFor(modelFactoryService.toUserModel(user));
  }
}
