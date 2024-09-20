package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder extends EntityBuilder<Answer> {
  private ReviewQuestionInstanceBuilder reviewQuestionInstanceBuilder = null;
  AnswerDTO answerDTO = new AnswerDTO();

  public AnswerBuilder(MakeMe makeMe) {
    super(makeMe, new Answer());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (this.reviewQuestionInstanceBuilder == null) {
      throw new RuntimeException("Question is required for Answer");
    }
    entity.setReviewQuestionInstance(reviewQuestionInstanceBuilder.please(needPersist));
    if (needPersist) {
      if (entity.getReviewQuestionInstance().getId() == null) {
        makeMe.modelFactoryService.save(entity.getReviewQuestionInstance());
      }
    }
    if (entity.getCorrect() == null) {
      if (answerDTO.getSpellingAnswer() == null && answerDTO.getChoiceIndex() == null) {
        answerDTO.setSpellingAnswer("spelling");
      }
      this.entity.setFromDTO(answerDTO);
    }
  }

  public AnswerBuilder withValidQuestion(PredefinedQuestionFactory predefinedQuestionFactory) {
    this.reviewQuestionInstanceBuilder =
        makeMe.aReviewQuestionInstance().useFactory(predefinedQuestionFactory);
    return this;
  }

  public AnswerBuilder ofSpellingQuestion(Note note) {
    this.reviewQuestionInstanceBuilder =
        makeMe.aReviewQuestionInstance().approvedSpellingQuestionOf(note);
    return this;
  }

  public AnswerBuilder forQuestion(ReviewQuestionInstance reviewQuestionInstance) {
    this.reviewQuestionInstanceBuilder = makeMe.theReviewQuestionInstance(reviewQuestionInstance);
    return this;
  }

  public AnswerBuilder answerWithSpelling(String answer) {
    answerDTO.setSpellingAnswer(answer);
    return this;
  }

  public AnswerBuilder choiceIndex(int index) {
    answerDTO.setChoiceIndex(index);
    return this;
  }

  public AnswerBuilder correct() {
    entity.setCorrect(true);
    return this;
  }
}
