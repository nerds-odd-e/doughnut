package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
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
        makeMe
            .modelFactoryService
            .toAnswerModel(answerBuilder.please(needPersist))
            .getAnswerViewedByUser(null);
  }

  public AnswerViewedByUserBuilder validQuestionOfType(
      QuizQuestionEntity.QuestionType questionType, ReviewPoint reviewPoint) {
    answerBuilder.withValidQuestion(questionType, reviewPoint);
    return this;
  }

  public AnswerViewedByUserBuilder answerWithSpelling(String answer) {
    this.answerBuilder.answerWithSpelling(answer);
    return this;
  }

  public AnswerViewedByUserBuilder forQuestion(QuizQuestionEntity quizQuestion) {
    answerBuilder.forQuestion(quizQuestion);
    return this;
  }

  public AnswerViewedByUserBuilder choiceIndex(int index) {
    this.answerBuilder.choiceIndex(index);
    return this;
  }
}
