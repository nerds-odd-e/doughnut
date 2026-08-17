package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

final class MemoryTrackerNextRecallScheduling {
  private MemoryTrackerNextRecallScheduling() {}

  static void apply(MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    tracker.setLastRecalledAt(currentUTCTimestamp);
    Timestamp scheduled = tracker.calculateNextRecallAt();
    if (!scheduled.after(currentUTCTimestamp)) {
      scheduled =
          TimestampOperations.addHoursToTimestamp(
              currentUTCTimestamp, Math.round(ForgettingCurve.FIRST_SUCCESS_STABILITY_HOURS));
    }
    tracker.setNextRecallAt(scheduled);
  }
}
