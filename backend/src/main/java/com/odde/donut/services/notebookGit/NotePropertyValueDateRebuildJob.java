package com.odde.donut.services.notebookGit;

import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class NotePropertyValueDateRebuildJob {
  private static final int NOTEBOOKS_PER_RUN = 10;

  private final NotePropertyValueDateRebuildService rebuildService;

  public NotePropertyValueDateRebuildJob(NotePropertyValueDateRebuildService rebuildService) {
    this.rebuildService = rebuildService;
  }

  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
  @SchedulerLock(
      name = "notePropertyValueDateRebuild",
      lockAtMostFor = "4m",
      lockAtLeastFor = "30s")
  public void run() {
    for (int i = 0; i < NOTEBOOKS_PER_RUN && rebuildService.rebuildOne(); i++) {}
  }
}
