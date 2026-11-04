package com.odde.donut.services.notebookGit;

import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class NotePropertyValueDateRebuildJob {
  private static final int NOTEBOOKS_PER_RUN = 10;

  private final NotePropertyValueDateRebuildService rebuildService;
  private final ApplicationAvailability availability;

  public NotePropertyValueDateRebuildJob(
      NotePropertyValueDateRebuildService rebuildService, ApplicationAvailability availability) {
    this.rebuildService = rebuildService;
    this.availability = availability;
  }

  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
  @SchedulerLock(
      name = "notePropertyValueDateRebuild",
      lockAtMostFor = "4m",
      lockAtLeastFor = "30s")
  public void run() {
    if (availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) {
      return;
    }
    for (int i = 0; i < NOTEBOOKS_PER_RUN && rebuildService.rebuildOne(); i++) {}
  }
}
