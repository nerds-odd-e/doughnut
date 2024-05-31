package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingQuizFactory;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestionEntity> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  public QuizQuestionBuilder buildValid(
      ReviewPoint reviewPoint, QuizQuestionFactory quizQuestionFactory) {
    QuizQuestionServant servant =
        new QuizQuestionServant(
            reviewPoint.getUser(), new NonRandomizer(), makeMe.modelFactoryService);
    try {
      this.entity = quizQuestionFactory.buildQuizQuestion(servant);
    } catch (QuizQuestionNotPossibleException e) {
      this.entity = null;
    }
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public QuizQuestion ViewedByUserPlease() {
    QuizQuestionEntity quizQuestion = inMemoryPlease();
    if (quizQuestion == null) return null;
    return quizQuestion.getQuizQuestion();
  }

  public QuizQuestionBuilder spellingQuestionOfNote(Note note) {
    return spellingQuestionOfReviewPoint(note);
  }

  public QuizQuestionBuilder spellingQuestionOfReviewPoint(Note note) {
    this.entity = new SpellingQuizFactory(note).buildSpellingQuestion();
    return this;
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    QuizQuestionEntity quizQuestionAIQuestion = new QuizQuestionEntity();
    quizQuestionAIQuestion.setNote(note);
    quizQuestionAIQuestion.setMcqWithAnswer(mcqWithAnswer);
    this.entity = quizQuestionAIQuestion;
    return this;
  }
}
