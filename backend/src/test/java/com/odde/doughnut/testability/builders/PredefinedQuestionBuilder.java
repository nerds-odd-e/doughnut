package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingPredefinedFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class PredefinedQuestionBuilder extends EntityBuilder<PredefinedQuestion> {
  public PredefinedQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      spellingQuestionOf(makeMe.aNote().please(needPersist));
    }
  }

  public PredefinedQuestionBuilder spellingQuestionOf(Note note) {
    this.entity = new SpellingPredefinedFactory(note).buildSpellingQuestion();
    this.entity.setApproved(false);
    return this;
  }

  public PredefinedQuestionBuilder approved() {
    this.entity.setApproved(true);
    return this;
  }

  public PredefinedQuestionBuilder approvedSpellingQuestionOf(Note note) {
    return spellingQuestionOf(note).approved();
  }

  public PredefinedQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.entity = PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }

  public PredefinedQuestionBuilder useFactory(SpellingPredefinedFactory predefinedQuestionFactory) {
    this.entity = predefinedQuestionFactory.buildSpellingQuestion();
    return this;
  }
}
