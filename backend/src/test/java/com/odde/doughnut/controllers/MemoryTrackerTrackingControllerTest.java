package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemoryTrackerTrackingControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

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

  @Test
  void markAsRecalledDoesNotDeleteTrackerWhenWrongAnswerThresholdExceeded() {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    addWrongAnswers(tracker, note, 5, day1);
    testabilitySettings.timeTravelTo(day1);

    controller.markAsRecalled(tracker, false);

    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        hasSize(1));
  }
}
