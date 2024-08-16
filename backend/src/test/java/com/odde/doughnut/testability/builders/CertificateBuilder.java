package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CertificateBuilder extends EntityBuilder<Certificate> {
  public CertificateBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, new Certificate());
    entity.setUser(assessmentAttempt.getUser());
    entity.setNotebook(assessmentAttempt.getNotebook());
    entity.setExpiryDate(
        TimestampOperations.addYearsToTimestamp(assessmentAttempt.getSubmittedAt(), 1));
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
