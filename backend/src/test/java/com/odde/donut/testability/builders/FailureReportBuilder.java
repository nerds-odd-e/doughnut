package com.odde.donut.testability.builders;

import com.odde.donut.entities.FailureReport;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class FailureReportBuilder extends EntityBuilder<FailureReport> {
  public FailureReportBuilder(MakeMe makeMe) {
    super(makeMe, new FailureReport());
    entity.setErrorName("errorName");
    entity.setErrorDetail("errorDetail");
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
