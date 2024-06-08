package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingQuizFactory;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestion> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  public QuizQuestionBuilder buildValid(
      ReviewPoint reviewPoint, QuizQuestionFactory quizQuestionFactory) {
    QuizQuestionServant servant =
        new QuizQuestionServant(
            reviewPoint.getUser(), new NonRandomizer(), makeMe.modelFactoryService);
    try {
      this.entity = quizQuestionFactory.buildValidQuizQuestion(servant);
    } catch (QuizQuestionNotPossibleException e) {
      this.entity = null;
    }
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public QuizQuestion ViewedByUserPlease() {
    QuizQuestion quizQuestion = inMemoryPlease();
    if (quizQuestion == null) return null;
    return quizQuestion;
  }

  public QuizQuestionBuilder spellingQuestionOfNote(Note note) {
    return spellingQuestionOfReviewPoint(note);
  }

  public QuizQuestionBuilder spellingQuestionOfReviewPoint(Note note) {
    this.entity = new SpellingQuizFactory(note).buildSpellingQuestion();
    return this;
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.entity = QuizQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }
}
