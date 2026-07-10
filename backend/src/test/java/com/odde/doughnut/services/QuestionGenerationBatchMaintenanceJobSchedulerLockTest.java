package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.lang.reflect.Method;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;

class QuestionGenerationBatchMaintenanceJobSchedulerLockTest {

  private static final String LOCK_NAME = "questionGenerationBatchHourlyMaintenance";

  @Test
  void hourlyMaintenanceUsesSchedulerLockWithHourlyDurations() throws NoSuchMethodException {
    Method method =
        QuestionGenerationBatchMaintenanceJob.class.getDeclaredMethod("runHourlyMaintenance");
    SchedulerLock schedulerLock = method.getAnnotation(SchedulerLock.class);

    assertThat(schedulerLock, notNullValue());
    assertThat(schedulerLock.name(), equalTo(LOCK_NAME));
    assertThat(schedulerLock.lockAtMostFor(), equalTo("55m"));
    assertThat(schedulerLock.lockAtLeastFor(), equalTo("1m"));
  }
}
