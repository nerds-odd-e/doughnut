package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class MarkedQuestionBuilder extends EntityBuilder<MarkedQuestion> {
  public MarkedQuestionBuilder(MakeMe makeMe) {
    super(makeMe, new MarkedQuestion());
    entity.setNoteId(makeMe.aNote().please().getId());
    entity.setUserId(makeMe.aUser().please().getId());
    entity.setQuizQuestionId(makeMe.aQuestion().please().getId());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
