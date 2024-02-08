package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionSpelling;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestionEntity> {
  private Note note;

  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  private void ofNote(Note note) {
    this.note = note;
  }

  public QuizQuestionBuilder buildValid(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    QuizQuestionGenerator builder =
        new QuizQuestionGenerator(
            reviewPoint.getUser(),
            reviewPoint.getNote(),
            new NonRandomizer(),
            makeMe.modelFactoryService,
            null);
    this.entity = builder.buildQuizQuestion(questionType).orElse(null);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      return;
    }

    if (note != null) {
      entity.setNote(note);
    }
  }

  public QuizQuestion ViewedByUserPlease() {
    QuizQuestionEntity quizQuestion = inMemoryPlease();
    if (quizQuestion == null) return null;
    return makeMe.modelFactoryService.toQuizQuestion(quizQuestion, makeMe.aUser().please());
  }

  public QuizQuestionBuilder spellingQuestionOfNote(Note note) {
    return spellingQuestionOfReviewPoint(note);
  }

  public QuizQuestionBuilder spellingQuestionOfReviewPoint(Note note) {
    ofNote(note);
    this.entity = new QuizQuestionSpelling();
    return this;
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    ofNote(note);
    this.entity = new QuizQuestionAIQuestion();
    entity.setRawJsonQuestion(mcqWithAnswer.toJsonString());
    entity.setCorrectAnswerIndex(mcqWithAnswer.correctChoiceIndex);
    return this;
  }
}
