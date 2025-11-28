package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionAnswer;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuestionAnswerBuilder extends EntityBuilder<QuestionAnswer> {
  private final PredefinedQuestionBuilder predefinedQuestionBuilder;
  private AnswerDTO answerDTO = null;

  public QuestionAnswerBuilder(MakeMe makeMe, QuestionAnswer questionAnswer) {
    super(makeMe, questionAnswer);
    predefinedQuestionBuilder = new PredefinedQuestionBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      entity = new QuestionAnswer();
      entity.setPredefinedQuestion(predefinedQuestionBuilder.please(needPersist));
    }
    if (answerDTO == null) {
      // Default to answer choice 0 if no answer provided
      answerDTO = new AnswerDTO();
      answerDTO.setChoiceIndex(0);
    }
    entity.buildAnswer(answerDTO);
  }

  public QuestionAnswerBuilder approvedQuestionOf(Note note) {
    this.predefinedQuestionBuilder.approvedQuestionOf(note);
    return this;
  }

  public QuestionAnswerBuilder ofAIGeneratedQuestion(MCQWithAnswer mcqWithAnswer, Note note) {
    this.predefinedQuestionBuilder.ofAIGeneratedQuestion(mcqWithAnswer, note);
    return this;
  }

  public QuestionAnswerBuilder answer(AnswerDTO answerDTO) {
    this.answerDTO = answerDTO;
    return this;
  }

  public QuestionAnswerBuilder answerChoiceIndex(int index) {
    AnswerDTO dto = new AnswerDTO();
    dto.setChoiceIndex(index);
    return answer(dto);
  }
}
