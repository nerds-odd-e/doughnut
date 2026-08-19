package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
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
  void successfulMarkAsRecalledLeavesOneGoodRecallLog() throws UnexpectedNoAccessRightException {
    Timestamp assimilatedAt = makeMe.aTimestamp().of(1, 8).please();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(ownedNote()).assimilatedAt(assimilatedAt).please();
    Timestamp recalledAt = makeMe.aTimestamp().of(2, 8).please();
    testabilitySettings.timeTravelTo(recalledAt);

    controller.markAsRecalled(tracker, true);

    List<RecallLog> logs = controller.getRecallLogs(tracker);
    assertThat(logs, hasSize(1));
    RecallLog log = logs.get(0);
    assertThat(log.getProductOutcome(), is(ProductOutcome.GOOD));
    assertThat(log.getRecordedAt(), equalTo(recalledAt));
    assertThat(log.getElapsedHours(), equalTo(0));
    assertThat(log.getAnswerId(), nullValue());
    assertThat(log.getMemoryTrackerId(), equalTo(tracker.getId()));
  }

  @Test
  void laterGradeOnGradedTrackerElapsedHoursAreSinceLastRecall()
      throws UnexpectedNoAccessRightException {
    Timestamp lastRecall = makeMe.aTimestamp().of(1, 8).please();
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(ownedNote())
            .assimilatedAt(lastRecall)
            .stabilityAndNextRecallAt(55f)
            .please();
    Timestamp recalledAt = makeMe.aTimestamp().of(2, 8).please();
    testabilitySettings.timeTravelTo(recalledAt);

    controller.markAsRecalled(tracker, true);

    assertThat(controller.getRecallLogs(tracker).get(0).getElapsedHours(), equalTo(24));
  }

  @Test
  void unsuccessfulMarkAsRecalledLeavesAnAgainRecallLog() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    makeMe.aRecallLogFor(tracker).please();

    controller.markAsRecalled(tracker, false);

    List<RecallLog> logs = controller.getRecallLogs(tracker);
    assertThat(logs, hasSize(2));
    assertThat(logs.get(0).getProductOutcome(), is(ProductOutcome.AGAIN));
  }

  @Test
  void markAsRecalledDoesNotDeleteTrackerWhenWrongAnswerThresholdExceeded() {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    addRecallLogs(tracker, ProductOutcome.AGAIN, 5, day1);
    testabilitySettings.timeTravelTo(day1);

    controller.markAsRecalled(tracker, false);

    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        hasSize(1));
  }
}
