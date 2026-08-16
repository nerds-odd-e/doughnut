package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static void recordFeedback(MemoryTracker tracker, Timestamp now, int score) {
    long elapsedInHours = TimestampOperations.getDiffInHours(now, tracker.getLastRecalledAt());
    tracker.setRecallCount(tracker.getRecallCount() + 1);
    tracker.setLastRecalledAt(now);
    tracker.setStability(nextStabilityHours(tracker, score, elapsedInHours));
    tracker.setNextRecallAt(ensureNextRecallStrictlyAfterNow(tracker, now));
  }

  private static float nextStabilityHours(MemoryTracker tracker, int score, long elapsedInHours) {
    if (score == 4 && tracker.getStability() > ForgettingCurve.ASSIMILATE_STABILITY_HOURS) {
      return tracker.stabilityHoursAfterSuccessfulRecall(elapsedInHours);
    }
    return CommissionedLearningSessionFeedbackPolicy.applyScore(tracker.getStability(), score);
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
