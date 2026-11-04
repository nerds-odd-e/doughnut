package com.odde.donut.services.notebookGit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;

@ExtendWith(MockitoExtension.class)
class NotePropertyValueDateRebuildJobTests {

  @Mock NotePropertyValueDateRebuildService rebuildService;

  NotePropertyValueDateRebuildJob job;

  @BeforeEach
  void setup() {
    var availability = new ApplicationAvailabilityBean();
    availability.onApplicationEvent(
        new AvailabilityChangeEvent<>(this, ReadinessState.ACCEPTING_TRAFFIC));
    job = new NotePropertyValueDateRebuildJob(rebuildService, availability);
  }

  @Test
  void stopsWhenNoNotebookIsLeftToDate() {
    when(rebuildService.rebuildOne()).thenReturn(true, true, false);

    job.run();

    verify(rebuildService, times(3)).rebuildOne();
  }

  @Test
  void datesAtMostTenNotebooksPerRun() {
    when(rebuildService.rebuildOne()).thenReturn(true);

    job.run();

    verify(rebuildService, times(10)).rebuildOne();
  }
}
