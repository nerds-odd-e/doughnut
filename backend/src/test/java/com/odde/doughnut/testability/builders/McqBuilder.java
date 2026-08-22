package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;

public class McqBuilder extends EntityBuilder<Mcq> {
  public McqBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      forNote(makeMe.aNote().please(needPersist));
    }
  }

  public McqBuilder forNote(Note note) {
    entity = new Mcq();
    entity.setNote(note);
    entity.setQuestionStem("a default question stem");
    entity.setResponseChoices(List.of("choice1", "choice2", "choice3"));
    entity.setCorrectAnswerIndex(0);
    return this;
  }

  public McqBuilder mcq(Mcq mcq) {
    entity = mcq;
    return this;
  }

  public McqBuilder stem(String stem) {
    entity.setQuestionStem(stem);
    return this;
  }

  public McqBuilder choices(String... choices) {
    return choices(List.of(choices));
  }

  public McqBuilder choices(List<String> choices) {
    entity.setResponseChoices(choices);
    return this;
  }

  public McqBuilder correctAnswerIndex(int correctAnswerIndex) {
    entity.setCorrectAnswerIndex(correctAnswerIndex);
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
