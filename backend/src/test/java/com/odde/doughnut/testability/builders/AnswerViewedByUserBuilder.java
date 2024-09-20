package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerViewedByUserBuilder extends EntityBuilder<AnsweredQuestion> {
  AnswerBuilder answerBuilder;

  public AnswerViewedByUserBuilder(MakeMe makeMe) {
    super(makeMe, null);
    answerBuilder = new AnswerBuilder(makeMe);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    this.entity =
        answerBuilder
            .reviewQuestionInstanceBuilder
            .answer(answerBuilder.answerDTO)
            .please(needPersist)
            .getAnsweredQuestion();
  }

  public AnswerViewedByUserBuilder validQuestionOfType(
      PredefinedQuestionFactory predefinedQuestionFactory) {
    answerBuilder.withValidQuestion(predefinedQuestionFactory);
    return this;
  }

  public AnswerViewedByUserBuilder answerWithSpelling(String answer) {
    this.answerBuilder.answerWithSpelling(answer);
    return this;
  }

  public AnswerViewedByUserBuilder forQuestion(ReviewQuestionInstance reviewQuestionInstance) {
    answerBuilder.forQuestion(reviewQuestionInstance);
    return this;
  }

  public AnswerViewedByUserBuilder choiceIndex(int index) {
    this.answerBuilder.choiceIndex(index);
    return this;
  }
}
