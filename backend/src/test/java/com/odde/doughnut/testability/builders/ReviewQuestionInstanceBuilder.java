package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ReviewQuestionInstanceBuilder extends EntityBuilder<ReviewQuestionInstance> {
  private final PredefinedQuestionBuilder predefinedQuestionBuilder;
  private AnswerDTO answerDTO = null;
  private boolean forceCorrectAnswer = false;

  public ReviewQuestionInstanceBuilder(
      MakeMe makeMe, ReviewQuestionInstance reviewQuestionInstance) {
    super(makeMe, reviewQuestionInstance);
    predefinedQuestionBuilder = new PredefinedQuestionBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) {
      entity = new ReviewQuestionInstance();
      entity.setPredefinedQuestion(predefinedQuestionBuilder.please(needPersist));
    }
    if (answerDTO != null) {
      entity.buildAnswer(answerDTO);
    }
    if (forceCorrectAnswer) {
      entity.getAnswer().setCorrect(true);
    }
  }

  public ReviewQuestionInstanceBuilder spellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.spellingQuestionOf(note);
    return this;
  }

  public ReviewQuestionInstanceBuilder approvedSpellingQuestionOf(Note note) {
    this.predefinedQuestionBuilder.approvedSpellingQuestionOf(note);
    return this;
  }

  public ReviewQuestionInstanceBuilder ofAIGeneratedQuestion(
      MCQWithAnswer mcqWithAnswer, Note note) {
    this.predefinedQuestionBuilder.ofAIGeneratedQuestion(mcqWithAnswer, note);
    return this;
  }

  public ReviewQuestionInstanceBuilder useFactory(
      PredefinedQuestionFactory predefinedQuestionFactory) {
    this.predefinedQuestionBuilder.useFactory(predefinedQuestionFactory);

    return this;
  }

  public ReviewQuestionInstanceBuilder answer(AnswerDTO answerDTO) {
    this.answerDTO = answerDTO;
    return this;
  }

  public ReviewQuestionInstanceBuilder answerChoiceIndex(int index) {
    AnswerDTO dto = new AnswerDTO();
    dto.setChoiceIndex(index);
    return answer(dto);
  }

  public ReviewQuestionInstanceBuilder answerSpelling(String answer) {
    AnswerDTO dto = new AnswerDTO();
    dto.setSpellingAnswer(answer);
    return answer(dto);
  }

  public ReviewQuestionInstanceBuilder forceAnswerCorrect() {
    this.forceCorrectAnswer = true;
    return answerSpelling("correct");
  }
}
