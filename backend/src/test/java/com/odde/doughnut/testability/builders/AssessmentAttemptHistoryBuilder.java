package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttemptHistory;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AssessmentAttemptHistoryBuilder extends EntityBuilder<AssessmentAttemptHistory> {

  public AssessmentAttemptHistoryBuilder(
      MakeMe makeMe, AssessmentAttemptHistory assessmentAttemptHistory) {
    super(makeMe, assessmentAttemptHistory);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
