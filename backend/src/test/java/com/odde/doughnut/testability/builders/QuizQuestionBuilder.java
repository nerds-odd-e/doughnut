package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<ReviewQuestionInstance> {
  private PredefinedQuestionBuilder predefinedQuestionBuilder;

  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
    predefinedQuestionBuilder = new PredefinedQuestionBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      entity = new ReviewQuestionInstance();
      entity.setPredefinedQuestion(predefinedQuestionBuilder.please(needPersist));
    }
  }

  public QuizQuestionBuilder spellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.spellingQuestionOf(note);
    return this;
  }

  public QuizQuestionBuilder approvedSpellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.approvedSpellingQuestionOf(note);
    return this;
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.predefinedQuestionBuilder.ofAIGeneratedQuestion(mcqWithAnswer, note);
    return this;
  }

  public QuizQuestionBuilder useFactory(QuizQuestionFactory quizQuestionFactory) {
    this.predefinedQuestionBuilder.useFactory(quizQuestionFactory);

    return this;
  }
}
