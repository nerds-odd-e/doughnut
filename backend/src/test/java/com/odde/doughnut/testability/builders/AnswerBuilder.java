package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder extends EntityBuilder<Answer> {
  private QuizQuestionBuilder quizQuestionBuilder = null;

  public AnswerBuilder(MakeMe makeMe) {
    super(makeMe, new Answer());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (this.quizQuestionBuilder != null) {
      entity.setReviewQuestionInstance(quizQuestionBuilder.please(needPersist));
    }
    if (needPersist) {
      if (entity.getReviewQuestionInstance().getId() == null) {
        makeMe.modelFactoryService.save(entity.getReviewQuestionInstance());
      }
    }
  }

  public AnswerBuilder withValidQuestion(QuizQuestionFactory quizQuestionFactory) {
    this.quizQuestionBuilder = makeMe.aQuizQuestion().useFactory(quizQuestionFactory);
    return this;
  }

  public AnswerBuilder ofSpellingQuestion(Note note) {
    this.quizQuestionBuilder = makeMe.aQuizQuestion().approvedSpellingQuestionOf(note);
    return this;
  }

  public AnswerBuilder forQuestion(ReviewQuestionInstance reviewQuestionInstance) {
    entity.setReviewQuestionInstance(reviewQuestionInstance);
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
