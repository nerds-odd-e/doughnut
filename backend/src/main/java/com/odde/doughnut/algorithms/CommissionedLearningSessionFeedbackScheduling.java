package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static void recordFeedback(MemoryTracker tracker, Timestamp now, int score) {
    tracker.setRecallCount(tracker.getRecallCount() + 1);
    tracker.setLastRecalledAt(now);
    tracker.setStability(
        CommissionedLearningSessionFeedbackPolicy.applyScore(tracker.getStability(), score));
    tracker.setNextRecallAt(ensureNextRecallStrictlyAfterNow(tracker, now));
  }

  private static Timestamp ensureNextRecallStrictlyAfterNow(MemoryTracker tracker, Timestamp now) {
    Timestamp scheduled = tracker.calculateNextRecallAt();
    if (scheduled.after(now)) {
      return scheduled;
    }
    return TimestampOperations.addHoursToTimestamp(now, firstPositiveSpacingHours(tracker));
  }

  private static int firstPositiveSpacingHours(MemoryTracker tracker) {
    SpacedRepetitionAlgorithm algorithm = tracker.getUser().getSpacedRepetitionAlgorithm();
    for (int spacingIndex = 0; spacingIndex < 30; spacingIndex++) {
      int hours = algorithm.getRepeatInHours(spacingIndex);
      if (hours > 0) {
        return hours;
      }
    }
    return 24;
  }
}
