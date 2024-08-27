package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AssessmentAttemptBuilder extends EntityBuilder<AssessmentAttempt> {

  public AssessmentAttemptBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, assessmentAttempt);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public AssessmentAttemptBuilder score(int totalQuestions, int correctAnswers) {
    this.entity.setAnswersTotal(totalQuestions);
    this.entity.setAnswersCorrect(correctAnswers);
    return this;
  }
}
