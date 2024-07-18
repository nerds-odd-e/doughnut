package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AssessmentAttemptHistoryBuilder extends EntityBuilder<AssessmentAttempt> {

  public AssessmentAttemptHistoryBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, assessmentAttempt);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
