package com.odde.donut.services.notebookGit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotePropertyValueDateRebuildJobTests {

  @Mock NotePropertyValueDateRebuildService rebuildService;

  NotePropertyValueDateRebuildJob job;

  @BeforeEach
  void setup() {
    job = new NotePropertyValueDateRebuildJob(rebuildService);
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
