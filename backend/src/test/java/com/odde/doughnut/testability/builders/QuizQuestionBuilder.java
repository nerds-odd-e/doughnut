package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestion> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, new QuizQuestion());
  }

  public QuizQuestionBuilder of(QuizQuestion.QuestionType questionType, ReviewPoint reviewPoint) {
    entity.setReviewPoint(reviewPoint);
    entity.setQuestionType(questionType);
    return this;
  }

  public QuizQuestionBuilder buildValid(
      QuizQuestion.QuestionType questionType, ReviewPoint reviewPoint) {
    QuizQuestionDirector builder =
        new QuizQuestionDirector(
            reviewPoint, questionType, new NonRandomizer(), makeMe.modelFactoryService, null);
    this.entity = builder.buildQuizQuestion().orElse(null);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public QuizQuestionViewedByUser ViewedByUserPlease() {
    QuizQuestion quizQuestion = inMemoryPlease();
    if (quizQuestion == null) return null;
    return QuizQuestionViewedByUser.create(
        quizQuestion, makeMe.modelFactoryService, makeMe.aNullUserModel().getEntity());
  }
}
