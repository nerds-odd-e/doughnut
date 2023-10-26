package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class SuggestedQuestionForFineTuningBuilder
    extends EntityBuilder<SuggestedQuestionForFineTuning> {
  private Note note = null;
  private MCQWithAnswer preservedQuestion = null;

  public SuggestedQuestionForFineTuningBuilder(MakeMe makeMe) {
    super(makeMe, new SuggestedQuestionForFineTuning());
    entity.setUser(makeMe.aUser().please());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    Note note = this.note == null ? makeMe.aNote().please() : this.note;
    entity.preserveNoteContent(note);
    if (this.preservedQuestion != null) {
      entity.setPreservedQuestion(this.preservedQuestion);
    } else {
      entity.setPreservedQuestion(makeMe.aMCQWithAnswer().please());
    }
  }

  public SuggestedQuestionForFineTuningBuilder ofNote(Note note) {
    this.note = note;
    return this;
  }

  public SuggestedQuestionForFineTuningBuilder withPreservedQuestion(MCQWithAnswer question) {
    this.preservedQuestion = question;
    return this;
  }

  public SuggestedQuestionForFineTuningBuilder positive() {
    entity.setPositiveFeedback(true);
    return this;
  }

  public SuggestedQuestionForFineTuningBuilder negative() {
    entity.setPositiveFeedback(false);
    return this;
  }
}
