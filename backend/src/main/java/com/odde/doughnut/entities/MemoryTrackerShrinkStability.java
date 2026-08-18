package com.odde.doughnut.entities;

import java.sql.Timestamp;

final class MemoryTrackerShrinkStability {
  private MemoryTrackerShrinkStability() {}

  static void apply(MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    if (tracker.forgettingCurve().isNewlyAssimilated()) {
      tracker.recalledHard(currentUTCTimestamp);
      return;
    }
    float initial = ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
    float accumulated = Math.max(0, tracker.getStability() - initial);
    tracker.setStability(Math.max(initial, Math.round(initial + accumulated * 0.8f)));
    tracker.scheduleNextRecallFromStability(currentUTCTimestamp);
  }
}
