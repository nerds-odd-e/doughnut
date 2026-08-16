package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static void recordFeedback(MemoryTracker tracker, Timestamp now, int score) {
    tracker.setRecallCount(tracker.getRecallCount() + 1);
    if (score == 4 || isScoreFiveOnNewlyAssimilated(tracker, score)) {
      tracker.recalledSuccessfully(now, null);
    } else if (score == 5) {
      tracker.recalledEasily(now);
    } else {
      tracker.setLastRecalledAt(now);
      tracker.setStability(
          CommissionedLearningSessionFeedbackPolicy.applyScore(tracker.getStability(), score));
    }
    tracker.setNextRecallAt(ensureNextRecallStrictlyAfterNow(tracker, now));
  }

  private static boolean isScoreFiveOnNewlyAssimilated(MemoryTracker tracker, int score) {
    return score == 5 && tracker.getStability() <= ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
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
