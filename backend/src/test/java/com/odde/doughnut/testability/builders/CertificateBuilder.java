package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.Calendar;

public class CertificateBuilder extends EntityBuilder<Certificate> {
  public CertificateBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, new Certificate());
    entity.setUser(assessmentAttempt.getUser());
    entity.setNotebook(assessmentAttempt.getNotebook());
    Timestamp expiryDate = assessmentAttempt.getSubmittedAt();
    Calendar cal = Calendar.getInstance();
    cal.setTime(expiryDate);
    cal.add(Calendar.YEAR, 1);
    expiryDate.setTime(cal.getTime().getTime());
    entity.setExpiryDate(expiryDate);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
