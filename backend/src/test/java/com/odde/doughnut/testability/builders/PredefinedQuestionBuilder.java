package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
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
      ofAIGeneratedQuestionForNote(makeMe.aNote().please(needPersist));
    }
  }

  public PredefinedQuestionBuilder approved() {
    this.entity.setApproved(true);
    return this;
  }

  public PredefinedQuestionBuilder approvedQuestionOf(Note note) {
    return ofAIGeneratedQuestionForNote(note).approved();
  }

  public PredefinedQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.entity = PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }

  public PredefinedQuestionBuilder ofAIGeneratedQuestionForNote(Note note) {
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswerBuilder().please();
    this.entity = PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }
}
