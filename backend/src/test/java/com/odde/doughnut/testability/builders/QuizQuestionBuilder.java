package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestionEntity> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, new QuizQuestionEntity());
  }

  public QuizQuestionBuilder of(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    entity.setThing(reviewPoint.getThing());
    entity.setQuestionType(questionType);
    return this;
  }

  public QuizQuestionBuilder buildValid(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    QuizQuestionGenerator builder =
        new QuizQuestionGenerator(
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

  public QuizQuestionBuilder aiQuestion(MCQWithAnswer MCQWithAnswer) {
    entity.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    entity.setCorrectAnswerIndex(MCQWithAnswer.correctChoiceIndex);
    return this;
  }

  public QuizQuestionBuilder ofNote(Note note) {
    return ofReviewPoint(
        makeMe.aReviewPointFor(note).by(note.getNotebook().getCreatorEntity()).please());
  }

  public QuizQuestionBuilder ofReviewPoint(ReviewPoint reviewPoint) {
    return of(QuizQuestionEntity.QuestionType.SPELLING, reviewPoint);
  }
}
