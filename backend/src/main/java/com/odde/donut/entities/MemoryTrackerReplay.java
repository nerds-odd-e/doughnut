package com.odde.donut.entities;

import java.util.List;

final class MemoryTrackerReplay {
  private MemoryTrackerReplay() {}

  /** Resets {@code tracker} to New and replays {@code chronological} events at their times. */
  static void rebuild(MemoryTracker tracker, List<RecallLog> chronological) {
    tracker.setStability(Fsrs.NEW_STABILITY_HOURS);
    tracker.setDifficulty(null);
    tracker.setLastRecalledAt(null);
    tracker.setNextRecallAt(tracker.calculateNextRecallAt());
    for (RecallLog log : chronological) {
      log.recordStateBefore(tracker);
      if (log.isConfusion()) {
        tracker.adjustForConfusion(log.getRecordedAt());
      } else {
        tracker.applyGrade(log.getRecordedAt(), log.getGrade());
      }
    }
  }
}
