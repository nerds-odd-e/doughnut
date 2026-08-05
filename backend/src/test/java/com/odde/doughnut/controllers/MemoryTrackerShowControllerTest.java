package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemoryTrackerShowControllerTest extends MemoryTrackerControllerTestBase {

  @Nested
  class WhenThereIsAMemoryTracker {
    MemoryTracker tracker;

    @BeforeEach
    void setup() {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      tracker = ownedTracker();
    }

    @Test
    void shouldBeAbleToSeeOwn() throws UnexpectedNoAccessRightException {
      assertThat(controller.showMemoryTracker(tracker), equalTo(tracker));
    }

    @Test
    void shouldExposeRecalledNote() throws UnexpectedNoAccessRightException {
      MemoryTracker shown = controller.showMemoryTracker(tracker);
      assertThat(
          shown.getRecalledNote().getNoteTopology().getId(), equalTo(tracker.getNote().getId()));
      assertThat(
          shown.getRecalledNote().getNotebookId(),
          equalTo(tracker.getNote().getNotebook().getId()));
    }

    @Test
    void shouldNotBeAbleToSeeOthers() {
      tracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.showMemoryTracker(tracker));
    }
  }
}
