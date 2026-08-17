package com.odde.doughnut.entities;

import java.sql.Timestamp;

final class MemoryTrackerConfusionAdjustment {
  private MemoryTrackerConfusionAdjustment() {}

  static void apply(MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    Timestamp existingDue = tracker.getNextRecallAt();
    tracker.setStability(
        tracker
            .forgettingCurve()
            .confusionAdjusted(tracker.elapsedHoursUntil(currentUTCTimestamp)));
    Timestamp projected = tracker.calculateNextRecallAt();
    tracker.setNextRecallAt(projected.after(existingDue) ? existingDue : projected);
  }
}
