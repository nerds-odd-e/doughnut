package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class FailureReportBuilder extends EntityBuilder<FailureReport> {
    static final TestObjectCounter titleCounter = new TestObjectCounter(n->"error" + n);

    @Override
    protected void beforeCreate(boolean needPersist) {
    }

    public FailureReportBuilder(FailureReport failureReport, MakeMe makeMe){
        super(makeMe, failureReport);
        errorName(titleCounter.generate());
        errorDetail("errorDetail");
        createdDatetime(new Timestamp(System.currentTimeMillis()));
    }



    public FailureReportBuilder errorName(String text) {
        entity.setErrorName(text);
        return this;
    }

    public FailureReportBuilder errorDetail(String text) {
        entity.setErrorDetail(text);
        return this;
    }

    public FailureReportBuilder createdDatetime(Timestamp timestamp) {
        entity.setCreateDatetime(timestamp);
        return this;
    }


}
