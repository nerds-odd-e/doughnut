package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class McqBuilder extends EntityBuilder<Mcq> {
  public McqBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      ofAIGeneratedQuestionForNote(makeMe.aNote().please(needPersist));
    }
  }

  public McqBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.entity = Mcq.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }

  public McqBuilder ofAIGeneratedQuestionForNote(Note note) {
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswerBuilder().please();
    this.entity = Mcq.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }

  public McqBuilder contested() {
    this.entity.setContested(true);
    return this;
  }

  public McqBuilder contextSeed(Long seed) {
    this.entity.setContextSeed(seed);
    return this;
  }
}
