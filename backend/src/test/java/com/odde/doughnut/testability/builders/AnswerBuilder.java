package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder extends EntityBuilder<Answer> {
  public AnswerBuilder(MakeMe makeMe) {
    super(makeMe, new Answer());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (needPersist) {
      if (entity.getQuizQuestion().getId() == null) {
        makeMe.modelFactoryService.save(entity.getQuizQuestion());
      }
    }
  }

  public AnswerBuilder withValidQuestion(QuizQuestionFactory quizQuestionFactory) {
    try {
      PredefinedQuestion predefinedQuestion = quizQuestionFactory.buildValidQuizQuestion();
      predefinedQuestion.getQuizQuestion().setPredefinedQuestion(predefinedQuestion);
      entity.setQuizQuestion(predefinedQuestion.getQuizQuestion());
    } catch (QuizQuestionNotPossibleException e) {
      throw new RuntimeException(
          "Failed to generate a question of type "
              + quizQuestionFactory.getClass().getSimpleName()
              + ", perhaps no enough data.");
    }
    return this;
  }

  public AnswerBuilder ofSpellingQuestion(Note note) {
    PredefinedQuestionBuilder predefinedQuestionBuilder = makeMe.aPredefinedQuestion();
    entity.setQuizQuestion(
        predefinedQuestionBuilder
            .approvedSpellingQuestionOf(note)
            .inMemoryPlease()
            .getQuizQuestion());
    return this;
  }

  public AnswerBuilder forQuestion(QuizQuestion quizQuestion) {
    entity.setQuizQuestion(quizQuestion);
    return this;
  }

  public AnswerBuilder answerWithSpelling(String answer) {
    this.entity.setChoiceIndex(null);
    this.entity.setSpellingAnswer(answer);
    return this;
  }

  public AnswerBuilder choiceIndex(int index) {
    this.entity.setSpellingAnswer(null);
    this.entity.setChoiceIndex(index);
    return this;
  }
}
