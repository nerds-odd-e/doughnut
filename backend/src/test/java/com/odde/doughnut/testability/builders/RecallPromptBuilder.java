package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingPredefinedFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class RecallPromptBuilder extends EntityBuilder<RecallPrompt> {
  private final PredefinedQuestionBuilder predefinedQuestionBuilder;
  private AnswerDTO answerDTO = null;

  public RecallPromptBuilder(MakeMe makeMe, RecallPrompt recallPrompt) {
    super(makeMe, recallPrompt);
    predefinedQuestionBuilder = new PredefinedQuestionBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      entity = new RecallPrompt();
      entity.setPredefinedQuestion(predefinedQuestionBuilder.please(needPersist));
    }
    if (answerDTO != null) {
      entity.buildAnswer(answerDTO);
    }
  }

  public RecallPromptBuilder spellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.spellingQuestionOf(note);
    return this;
  }

  public RecallPromptBuilder approvedSpellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.approvedSpellingQuestionOf(note);
    return this;
  }

  public RecallPromptBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.predefinedQuestionBuilder.ofAIGeneratedQuestion(mcqWithAnswer, note);
    return this;
  }

  public RecallPromptBuilder useFactory(SpellingPredefinedFactory predefinedQuestionFactory) {
    this.predefinedQuestionBuilder.useFactory(predefinedQuestionFactory);

    return this;
  }

  public RecallPromptBuilder answer(AnswerDTO answerDTO) {
    this.answerDTO = answerDTO;
    return this;
  }

  public RecallPromptBuilder answerChoiceIndex(int index) {
    AnswerDTO dto = new AnswerDTO();
    dto.setChoiceIndex(index);
    return answer(dto);
  }

  public RecallPromptBuilder answerSpelling(String answer) {
    AnswerDTO dto = new AnswerDTO();
    dto.setSpellingAnswer(answer);
    return answer(dto);
  }
}
