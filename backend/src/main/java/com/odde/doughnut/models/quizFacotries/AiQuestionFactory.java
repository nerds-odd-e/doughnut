package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;

public class AiQuestionFactory implements QuizQuestionFactory {
  private ReviewPoint reviewPoint;
  private QuizQuestionServant servant;

  public AiQuestionFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.servant = servant;
  }

  @Override
  public void fillQuizQuestion(QuizQuestion quizQuestion) {
    String suggestion =
        servant.aiAdvisorService.generateQuestionJsonString(
            reviewPoint.getNote(), servant.modelFactoryService);
    quizQuestion.setRawJsonQuestion(suggestion);
  }
}
