package com.odde.donut.entities;

import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;

final class MemoryTrackerRecallDue {
  private static final int MINIMUM_FUTURE_INTERVAL_HOURS = 1;

  private MemoryTrackerRecallDue() {}

  static Timestamp calculateNextRecallAt(MemoryTracker tracker) {
    Timestamp lastRecalledAt = tracker.getLastRecalledAt();
    if (lastRecalledAt == null) {
      return tracker.getAssimilatedAt();
    }
    return TimestampOperations.addHoursToTimestamp(
        lastRecalledAt, Fsrs.intervalHours(tracker.getStability()));
  }

  static long elapsedHoursUntil(MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    Timestamp lastRecalledAt = tracker.getLastRecalledAt();
    if (lastRecalledAt == null) {
      return 0;
    }
    return TimestampOperations.getDiffInHours(currentUTCTimestamp, lastRecalledAt);
  }

  static void scheduleNextRecallFromStability(
      MemoryTracker tracker, Timestamp currentUTCTimestamp) {
    tracker.setLastRecalledAt(currentUTCTimestamp);
    Timestamp scheduled =
        TimestampOperations.addHoursToTimestamp(
            currentUTCTimestamp, scheduledIntervalHours(tracker, currentUTCTimestamp));
    if (!scheduled.after(currentUTCTimestamp)) {
      scheduled =
          TimestampOperations.addHoursToTimestamp(
              currentUTCTimestamp, Fsrs.intervalHours(Fsrs.STRICTLY_FUTURE_FALLBACK_HOURS));
    }
    tracker.setNextRecallAt(scheduled);
  }

  private static int scheduledIntervalHours(MemoryTracker tracker, Timestamp gradeAt) {
    return MemoryTrackerRecallIntervalFuzz.intervalHoursForGrade(
        tracker.getId(),
        gradeAt,
        tracker.getStability(),
        MINIMUM_FUTURE_INTERVAL_HOURS,
        Fsrs.intervalHours(Fsrs.MAXIMUM_INTERVAL_HOURS));
  }
}
