package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static void recordFeedback(MemoryTracker tracker, Timestamp now, int score) {
    tracker.setRecallCount(tracker.getRecallCount() + 1);
    if (score == 4) {
      tracker.recalledSuccessfully(now, null);
    } else if (score == 5) {
      tracker.recalledEasily(now);
    } else if (score == 3) {
      tracker.recalledHard(now);
    } else if (score == 1) {
      tracker.recalledAgain(now);
    } else {
      tracker.setLastRecalledAt(now);
      tracker.setStability(
          CommissionedLearningSessionFeedbackPolicy.applyScore(tracker.getStability(), score));
    }
    tracker.setNextRecallAt(ensureNextRecallStrictlyAfterNow(tracker, now));
  }

  private static Timestamp ensureNextRecallStrictlyAfterNow(MemoryTracker tracker, Timestamp now) {
    Timestamp scheduled = tracker.calculateNextRecallAt();
    if (scheduled.after(now)) {
      return scheduled;
    }
    return TimestampOperations.addHoursToTimestamp(
        now, SpacedRepetitionAlgorithm.hoursFromSpacingIndex(1));
  }
}
