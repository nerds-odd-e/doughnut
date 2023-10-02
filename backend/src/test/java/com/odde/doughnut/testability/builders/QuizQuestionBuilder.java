package com.odde.doughnut.testability.builders;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.AI_QUESTION;

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

  private void ofReviewPoint(ReviewPoint reviewPoint) {
    entity.setThing(reviewPoint.getThing());
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

  public QuizQuestionBuilder ofNote(Note note) {
    return spellingQuestionOfReviewPoint(
        makeMe.aReviewPointFor(note).by(note.getNotebook().getCreatorEntity()).please());
  }

  public QuizQuestionBuilder spellingQuestionOfReviewPoint(ReviewPoint reviewPoint) {
    ofReviewPoint(reviewPoint);
    entity.setQuestionType(QuizQuestionEntity.QuestionType.SPELLING);
    return this;
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(
      ReviewPoint reviewPoint1, MCQWithAnswer mcqWithAnswer1) {
    ofReviewPoint(reviewPoint1);
    entity.setQuestionType(AI_QUESTION);
    entity.setRawJsonQuestion(mcqWithAnswer1.toJsonString());
    entity.setCorrectAnswerIndex(mcqWithAnswer1.correctChoiceIndex);
    return this;
  }
}
