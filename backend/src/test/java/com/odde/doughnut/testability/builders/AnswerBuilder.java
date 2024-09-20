package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AnswerBuilder extends EntityBuilder<Answer> {
  public ReviewQuestionInstanceBuilder reviewQuestionInstanceBuilder = null;

  public AnswerBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (this.reviewQuestionInstanceBuilder == null) {
      throw new RuntimeException("Question is required for Answer");
    }
    ReviewQuestionInstance reviewQuestionInstance =
        reviewQuestionInstanceBuilder.please(needPersist);
    entity = reviewQuestionInstance.getAnswer();
  }

  public AnswerBuilder answered() {
    reviewQuestionInstanceBuilder.answerSpelling("spelling");
    return this;
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
    reviewQuestionInstanceBuilder.answerSpelling(answer);
    return this;
  }

  public AnswerBuilder choiceIndex(int index) {
    reviewQuestionInstanceBuilder.answerChoiceIndex(index);
    return this;
  }

  public AnswerBuilder correct() {
    reviewQuestionInstanceBuilder.forceAnswerCorrect();
    return this;
  }

  public AnsweredQuestion ooo() {
    return reviewQuestionInstanceBuilder.please(false).getAnsweredQuestion();
  }
}
