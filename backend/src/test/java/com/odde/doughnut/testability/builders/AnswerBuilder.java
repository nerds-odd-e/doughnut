package com.odde.doughnut.testability.builders;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder extends EntityBuilder<Answer> {
  public ReviewQuestionInstanceBuilder reviewQuestionInstanceBuilder = null;
  AnswerDTO answerDTO = new AnswerDTO();
  Boolean forceCorrect = null;

  public AnswerBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (this.reviewQuestionInstanceBuilder == null) {
      throw new RuntimeException("Question is required for Answer");
    }
    if (answerDTO.getSpellingAnswer() == null && answerDTO.getChoiceIndex() == null) {
      answerDTO.setSpellingAnswer("spelling");
    }
    ReviewQuestionInstance reviewQuestionInstance =
        reviewQuestionInstanceBuilder.answer(answerDTO).please(needPersist);
    entity = reviewQuestionInstance.getAnswer();
    if (forceCorrect != null) {
      entity.setCorrect(forceCorrect);
    }
  }

  public AnswerBuilder withValidQuestion(PredefinedQuestionFactory predefinedQuestionFactory) {
    this.reviewQuestionInstanceBuilder =
        makeMe.aReviewQuestionInstance().useFactory(predefinedQuestionFactory);
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
    forceCorrect = true;
    return this;
  }

  public AnsweredQuestion ooo() {
    return reviewQuestionInstanceBuilder.answer(answerDTO).please(false).getAnsweredQuestion();
  }
}
