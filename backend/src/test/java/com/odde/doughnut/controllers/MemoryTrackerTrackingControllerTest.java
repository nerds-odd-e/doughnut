package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class MemoryTrackerTrackingControllerTest extends MemoryTrackerControllerTestBase {

  @Test
  void removeAndUpdateLastRecalledAt() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    MemoryTracker tracker = ownedTracker();
    controller.removeFromRepeating(tracker);
    assertThat(tracker.getRemovedFromTracking(), is(true));
    assertThat(tracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
  }

  @Test
  void reEnableShouldSetRemovedFromTrackingToFalse() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(ownedNote()).removedFromTracking().please();
    controller.reEnable(tracker);
    assertThat(tracker.getRemovedFromTracking(), is(false));
  }

  @Test
  void shouldNotBeAbleToReEnableOthersMemoryTracker() {
    MemoryTracker tracker =
        makeMe.aMemoryTrackerBy(makeMe.aUser().please()).removedFromTracking().please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.reEnable(tracker));
  }

  @Test
  void markAsRecalledIncrementsRecallCount() {
    MemoryTracker tracker = ownedTracker();
    Integer oldRecallCount = tracker.getRecallCount();
    controller.markAsRecalled(tracker, true);
    assertThat(tracker.getRecallCount(), equalTo(oldRecallCount + 1));
  }
}
