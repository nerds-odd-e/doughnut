package com.odde.doughnut.entities;

import java.sql.Timestamp;

final class MemoryTrackerAgainRecall {
  private MemoryTrackerAgainRecall() {}

  static void apply(MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    ForgettingCurve curve = tracker.forgettingCurve();
    if (tracker.getStability() > ForgettingCurve.ASSIMILATE_STABILITY_HOURS) {
      tracker.setDifficulty(curve.difficultyAfterFailedRecall());
    }
    tracker.setStability(curve.failed(tracker.elapsedHoursUntil(currentUTCTimestamp)));
    tracker.scheduleNextRecallFromStability(currentUTCTimestamp);
  }
}
