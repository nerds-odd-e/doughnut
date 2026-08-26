package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerTrackingControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void removeDoesNotChangeLastRecalledAt() {
    Timestamp lastRecall = makeMe.aTimestamp().of(1, 8).please();
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(ownedNote())
            .assimilatedAt(lastRecall)
            .stabilityAndNextRecallAt(55f)
            .please();
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 8).please());

    controller.removeFromRepeating(tracker);

    assertThat(tracker.getRemovedFromTracking(), is(true));
    assertThat(tracker.getLastRecalledAt(), equalTo(lastRecall));
  }

  @Test
  void reEnableDoesNotChangeLastRecalledAt() throws UnexpectedNoAccessRightException {
    Timestamp lastRecall = makeMe.aTimestamp().of(1, 8).please();
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(ownedNote())
            .assimilatedAt(lastRecall)
            .stabilityAndNextRecallAt(55f)
            .removedFromTracking()
            .please();
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 8).please());

    controller.reEnable(tracker);

    assertThat(tracker.getLastRecalledAt(), equalTo(lastRecall));
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
  void markAsRecalledWithGoodLeavesOneGoodRecallLog() throws UnexpectedNoAccessRightException {
    Timestamp assimilatedAt = makeMe.aTimestamp().of(1, 8).please();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(ownedNote()).assimilatedAt(assimilatedAt).please();
    Timestamp recalledAt = makeMe.aTimestamp().of(2, 8).please();
    testabilitySettings.timeTravelTo(recalledAt);

    controller.markAsRecalled(tracker, Grade.GOOD);

    List<RecallLog> logs = controller.getRecallLogs(tracker);
    assertThat(logs, hasSize(1));
    RecallLog log = logs.get(0);
    assertThat(log.getGrade(), is(Grade.GOOD));
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

    controller.markAsRecalled(tracker, Grade.GOOD);

    assertThat(controller.getRecallLogs(tracker).get(0).getElapsedHours(), equalTo(24));
  }

  @Test
  void markAsRecalledWithAgainLeavesAnAgainRecallLog() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    makeMe.aRecallLogFor(tracker).please();

    controller.markAsRecalled(tracker, Grade.AGAIN);

    List<RecallLog> logs = controller.getRecallLogs(tracker);
    assertThat(logs, hasSize(2));
    assertThat(logs.get(0).getGrade(), is(Grade.AGAIN));
  }

  @ParameterizedTest
  @EnumSource(
      value = Grade.class,
      names = {"HARD", "EASY"})
  void markAsRecalledRejectsHardAndEasy(Grade grade) {
    MemoryTracker tracker = ownedTracker();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.markAsRecalled(tracker, grade));

    assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void markAsRecalledDoesNotDeleteTrackerWhenWrongAnswerThresholdExceeded() {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    addRecallLogs(tracker, Grade.AGAIN, 5, day1);
    testabilitySettings.timeTravelTo(day1);

    controller.markAsRecalled(tracker, Grade.AGAIN);

    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        hasSize(1));
  }
}
