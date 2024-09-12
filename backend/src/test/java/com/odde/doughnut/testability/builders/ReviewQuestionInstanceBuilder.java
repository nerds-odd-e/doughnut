package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ReviewQuestionInstanceBuilder extends EntityBuilder<ReviewQuestionInstance> {
  private final PredefinedQuestionBuilder predefinedQuestionBuilder;

  public ReviewQuestionInstanceBuilder(MakeMe makeMe) {
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

  public ReviewQuestionInstanceBuilder spellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.spellingQuestionOf(note);
    return this;
  }

  public ReviewQuestionInstanceBuilder approvedSpellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.approvedSpellingQuestionOf(note);
    return this;
  }

  public ReviewQuestionInstanceBuilder ofAIGeneratedQuestion(
      MCQWithAnswer mcqWithAnswer, Note note) {
    this.predefinedQuestionBuilder.ofAIGeneratedQuestion(mcqWithAnswer, note);
    return this;
  }

  public ReviewQuestionInstanceBuilder useFactory(
      PredefinedQuestionFactory predefinedQuestionFactory) {
    this.predefinedQuestionBuilder.useFactory(predefinedQuestionFactory);

    return this;
  }
}
