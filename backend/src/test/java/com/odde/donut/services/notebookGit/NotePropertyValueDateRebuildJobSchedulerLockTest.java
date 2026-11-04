package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

class NotePropertyValueDateRebuildJobSchedulerLockTest {

  @Test
  void runsEveryFiveMinutesInProductionUnderOneSchedulerLock() throws NoSuchMethodException {
    Method method = NotePropertyValueDateRebuildJob.class.getDeclaredMethod("run");
    Scheduled scheduled = method.getAnnotation(Scheduled.class);
    SchedulerLock schedulerLock = method.getAnnotation(SchedulerLock.class);

    assertThat(
        NotePropertyValueDateRebuildJob.class.getAnnotation(Profile.class).value(),
        arrayContaining("prod"));
    assertThat(scheduled.fixedDelay(), equalTo(5L));
    assertThat(scheduled.timeUnit(), equalTo(TimeUnit.MINUTES));
    assertThat(schedulerLock, notNullValue());
    assertThat(schedulerLock.name(), equalTo("notePropertyValueDateRebuild"));
    assertThat(schedulerLock.lockAtMostFor(), equalTo("4m"));
    assertThat(schedulerLock.lockAtLeastFor(), equalTo("30s"));
  }
}
