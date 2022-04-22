package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;

public abstract class AbstractCategoryQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final ReviewPoint reviewPoint;
  protected final Link link;
  protected final QuizQuestionServant servant;

  public AbstractCategoryQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.link = reviewPoint.getLink();
    this.servant = servant;
  }
}
