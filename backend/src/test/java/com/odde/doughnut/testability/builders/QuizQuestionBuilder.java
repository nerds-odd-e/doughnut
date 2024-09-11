package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionAndAnswer;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingQuizFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuestionAndAnswer> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      spellingQuestionOf(makeMe.aNote().please(needPersist));
    }
  }

  public QuizQuestionBuilder spellingQuestionOf(Note note) {
    this.entity = new SpellingQuizFactory(note).buildSpellingQuestion();
    this.entity.setApproved(false);
    return this;
  }

  public QuizQuestionBuilder approved() {
    this.entity.setApproved(true);
    return this;
  }

  public QuizQuestionBuilder approvedSpellingQuestionOf(Note note) {
    return spellingQuestionOf(note).approved();
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.entity = QuestionAndAnswer.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }
}
