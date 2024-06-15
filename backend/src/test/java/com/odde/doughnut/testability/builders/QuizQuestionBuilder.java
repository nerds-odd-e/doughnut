package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingQuizFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuizQuestionBuilder extends EntityBuilder<QuizQuestion> {
  public QuizQuestionBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public QuizQuestionBuilder spellingQuestionOfNote(Note note) {
    return spellingQuestionOfReviewPoint(note);
  }

  public QuizQuestionBuilder spellingQuestionOfReviewPoint(Note note) {
    this.entity = new SpellingQuizFactory(note).buildSpellingApprovedQuestion();
    return this;
  }

  public QuizQuestionBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.entity = QuizQuestion.fromMCQWithAnswer(mcqWithAnswer, note);
    return this;
  }
}
