package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestionEntity> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, new QuizQuestionEntity());
  }

  public QuizQuestionBuilder of(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    entity.setReviewPoint(reviewPoint);
    entity.setQuestionType(questionType);
    return this;
  }

  public QuizQuestionBuilder buildValid(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    QuizQuestionDirector builder =
        new QuizQuestionDirector(
            reviewPoint, new NonRandomizer(), makeMe.modelFactoryService, null);
    this.entity = builder.buildQuizQuestion(questionType).orElse(null);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public QuizQuestion ViewedByUserPlease() {
    QuizQuestionEntity quizQuestion = inMemoryPlease();
    if (quizQuestion == null) return null;
    return makeMe.modelFactoryService.toQuizQuestion(quizQuestion, makeMe.aUser().please());
  }
}
