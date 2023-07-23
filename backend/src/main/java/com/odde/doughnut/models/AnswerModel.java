package com.odde.doughnut.models;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.util.Objects;

public class AnswerModel {
  private final Answer answer;
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

  public AnswerResult getAnswerResult() {
    AnswerResult answerResult = new AnswerResult();
    answerResult.answerId = answer.getId();
    answerResult.correct = isCorrect();
    return answerResult;
  }

  public void save() {
    modelFactoryService.answerRepository.save(answer);
  }

  private boolean isCorrect() {
    QuizQuestionEntity question = answer.getQuestion();
    if (question.getCorrectAnswerIndex() != null) {
      return Objects.equals(answer.getChoiceIndex(), question.getCorrectAnswerIndex());
    }
    return question.buildPresenter().isAnswerCorrect(answer);
  }

  public AnswerViewedByUser getAnswerViewedByUser(User user) {
    AnswerViewedByUser answerResult = new AnswerViewedByUser();
    answerResult.answerResult = getAnswerResult();
    answerResult.answerDisplay = answer.getAnswerDisplay(modelFactoryService);
    answerResult.reviewPoint = answer.getQuestion().getReviewPoint();
    QuizQuestionEntity quizQuestion = answer.getQuestion();
    answerResult.quizQuestion = modelFactoryService.toQuizQuestion(quizQuestion, user);
    return answerResult;
  }
}
