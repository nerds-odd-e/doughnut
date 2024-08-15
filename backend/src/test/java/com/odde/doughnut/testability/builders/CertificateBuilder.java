package com.odde.doughnut.testability.builders;

import com.odde.doughnut.algorithms.TimestampUtil;
import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CertificateBuilder extends EntityBuilder<Certificate> {
  public CertificateBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, new Certificate());
    entity.setUser(assessmentAttempt.getUser());
    entity.setNotebook(assessmentAttempt.getNotebook());
    entity.setExpiryDate(TimestampUtil.addYearsToTimestamp(assessmentAttempt.getSubmittedAt()));
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
