package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class SuggestedQuestionForFineTuningBuilder
    extends EntityBuilder<SuggestedQuestionForFineTuning> {
  public SuggestedQuestionForFineTuningBuilder(MakeMe makeMe) {
    super(makeMe, new SuggestedQuestionForFineTuning());
    ofNote(makeMe.aNote().please());
    entity.setUser(makeMe.aUser().please());
    entity.setQuizQuestion(makeMe.aQuestion().please());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public SuggestedQuestionForFineTuningBuilder ofNote(Note note) {
    entity.setNote(note);
    return this;
  }

  public SuggestedQuestionForFineTuningBuilder withRawQuestion(String question) {
    entity.getQuizQuestion().setRawJsonQuestion(question);
    return this;
  }
}
