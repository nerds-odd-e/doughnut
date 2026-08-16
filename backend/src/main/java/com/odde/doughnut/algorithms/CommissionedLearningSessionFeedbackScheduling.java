package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static void recordFeedback(MemoryTracker tracker, Timestamp now, int score) {
    tracker.setRecallCount(tracker.getRecallCount() + 1);
    switch (score) {
      case 5 -> tracker.recalledEasily(now);
      case 4 -> tracker.recalledSuccessfully(now, null);
      case 3 -> tracker.recalledHard(now);
      case 2 -> tracker.shrinkStability(now);
      case 1, 0 -> tracker.recalledAgain(now);
    }
    tracker.setNextRecallAt(ensureNextRecallStrictlyAfterNow(tracker, now));
  }

  private static Timestamp ensureNextRecallStrictlyAfterNow(MemoryTracker tracker, Timestamp now) {
    Timestamp scheduled = tracker.calculateNextRecallAt();
    if (scheduled.after(now)) {
      return scheduled;
    }
    return TimestampOperations.addHoursToTimestamp(
        now, Math.round(ForgettingCurve.FIRST_SUCCESS_STABILITY_HOURS));
  }
}
