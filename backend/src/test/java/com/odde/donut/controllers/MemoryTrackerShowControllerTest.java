package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
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
    void ordinaryTrackerDefaultsToUnderstandingType() throws UnexpectedNoAccessRightException {
      MemoryTracker shown = controller.showMemoryTracker(tracker);
      assertThat(shown.getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    }

    @Test
    void spellingTrackerHasSpellingType() throws UnexpectedNoAccessRightException {
      MemoryTracker spellingTracker = makeMe.aMemoryTrackerFor(ownedNote()).spelling().please();
      MemoryTracker shown = controller.showMemoryTracker(spellingTracker);
      assertThat(shown.getType(), equalTo(MemoryTrackerType.SPELLING));
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

  @Nested
  class DifficultyPersistence {
    @Test
    void persistsAssignedDifficulty() {
      MemoryTracker stored = makeMe.aMemoryTrackerFor(ownedNote()).difficulty(7f).please();

      assertThat(reloaded(stored).getDifficulty(), equalTo(7f));
    }

    @Test
    void leavesDifficultyUnsetForAssimilateOnlyTracker() {
      MemoryTracker stored = makeMe.aMemoryTrackerFor(ownedNote()).please();

      assertThat(reloaded(stored).getDifficulty(), nullValue());
    }

    private MemoryTracker reloaded(MemoryTracker stored) {
      Integer id = stored.getId();
      makeMe.entityPersister.flushAndClear();
      return makeMe.entityPersister.find(MemoryTracker.class, id);
    }
  }
}
