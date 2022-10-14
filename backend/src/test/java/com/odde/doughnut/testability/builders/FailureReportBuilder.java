package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class FailureReportBuilder extends EntityBuilder<FailureReport> {
  public FailureReportBuilder(MakeMe makeMe) {
    super(makeMe, new FailureReport());
    entity.setErrorName("errorName");
    entity.setErrorDetail("errorDetail");
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
