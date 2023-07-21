package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;

public class AiQuestionFactory implements QuizQuestionFactory, QuestionRawJsonFactory {
  private ReviewPoint reviewPoint;
  private QuizQuestionServant servant;

  public AiQuestionFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.servant = servant;
  }

  @Override
  public String generateRawJsonQuestion() throws QuizQuestionNotPossibleException {
    return servant.aiAdvisorService.generateQuestion(reviewPoint.getNote()).toJsonString();
  }
}
