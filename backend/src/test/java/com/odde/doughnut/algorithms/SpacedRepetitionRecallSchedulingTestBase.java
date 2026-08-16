package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_DIFFICULTY;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

abstract class SpacedRepetitionRecallSchedulingTestBase {
  static final float STABILITY_HOURS = 72f;
  final MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  final User user = makeMe.aUser().inMemoryPlease();
  final Note note = makeMe.aNote().inMemoryPlease();

  MemoryTracker aGradedTrackerAtThreeDayStability() {
    return aGradedTrackerAtStability(STABILITY_HOURS);
  }

  MemoryTracker aGradedTrackerAtThreeDayStability(float difficulty) {
    return aGradedTrackerAtStability(STABILITY_HOURS, difficulty);
  }

  MemoryTracker aGradedTrackerAtStability(float stabilityHours) {
    return aGradedTrackerAtStability(stabilityHours, DEFAULT_DIFFICULTY);
  }

  MemoryTracker aGradedTrackerAtStability(float stabilityHours, float difficulty) {
    return makeMe
        .aMemoryTrackerFor(note)
        .by(user)
        .stabilityAndNextRecallAt(stabilityHours)
        .difficulty(difficulty)
        .inMemoryPlease();
  }

  Timestamp onTimeGradeTime(MemoryTracker tracker) {
    return TimestampOperations.addHoursToTimestamp(
        tracker.getLastRecalledAt(), Math.round(tracker.getStability()));
  }

  Timestamp overdueGradeTime(MemoryTracker tracker) {
    return TimestampOperations.addHoursToTimestamp(
        tracker.getLastRecalledAt(), Math.round(tracker.getStability()) * 2);
  }
}
