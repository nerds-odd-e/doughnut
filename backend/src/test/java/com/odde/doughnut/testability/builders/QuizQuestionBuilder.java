package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.AIGeneratedQuestion;
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

  public QuizQuestionBuilder aiQuestion(AIGeneratedQuestion aiGeneratedQuestion) {
    entity.setRawJsonQuestion(aiGeneratedQuestion.toJsonString());
    entity.setCorrectAnswerIndex(aiGeneratedQuestion.correctChoiceIndex);
    return this;
  }

  public QuizQuestionBuilder correctAnswerIndex(int index) {
    entity.setCorrectAnswerIndex(index);
    return this;
  }
}
