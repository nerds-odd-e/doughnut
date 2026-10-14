package com.odde.donut.entities;

import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;

final class MemoryTrackerReplay {
  private MemoryTrackerReplay() {}

  /**
   * Rebuilds {@code tracker} as if {@code excluded}, one of the logs in its chronological {@code
   * history}, had not happened: from the memory state cached on {@code excluded}, replaying the
   * valid events recorded after it at their times. When {@code excluded} holds no cached state, all
   * valid events are replayed from a New tracker.
   */
  static void rebuildWithout(MemoryTracker tracker, RecallLog excluded, List<RecallLog> history) {
    if (excluded.getStabilityBefore() == null) {
      replay(tracker, Fsrs.NEW_STABILITY_HOURS, null, null, history);
      return;
    }
    int at = history.indexOf(excluded);
    replay(
        tracker,
        excluded.getStabilityBefore(),
        excluded.getDifficultyBefore(),
        lastRecallBefore(excluded, history.subList(0, at)),
        history.subList(at + 1, history.size()));
  }

  /** The latest earlier valid Grade when one is logged, else what the elapsed hours imply. */
  private static Timestamp lastRecallBefore(RecallLog excluded, List<RecallLog> earlier) {
    if (Fsrs.isNew(excluded.getStabilityBefore())) {
      return null;
    }
    return earlier.reversed().stream()
        .filter(log -> log.isValidEvent() && !log.isConfusion())
        .findFirst()
        .map(RecallLog::getRecordedAt)
        .orElseGet(
            () ->
                TimestampOperations.addHoursToTimestamp(
                    excluded.getRecordedAt(), -excluded.getElapsedHours()));
  }

  private static void replay(
      MemoryTracker tracker,
      float stability,
      Float difficulty,
      Timestamp lastRecalledAt,
      List<RecallLog> events) {
    tracker.setStability(stability);
    tracker.setDifficulty(difficulty);
    if (lastRecalledAt == null) {
      tracker.setLastRecalledAt(null);
      tracker.setNextRecallAt(tracker.calculateNextRecallAt());
    } else {
      MemoryTrackerRecallDue.scheduleNextRecallFromStability(tracker, lastRecalledAt);
    }
    for (RecallLog log : events.stream().filter(RecallLog::isValidEvent).toList()) {
      log.recordStateBefore(tracker);
      if (log.isConfusion()) {
        tracker.adjustForConfusion(log.getRecordedAt());
      } else {
        tracker.applyGrade(log.getRecordedAt(), log.getGrade());
      }
    }
  }
}
