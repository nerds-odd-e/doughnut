package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.MemoryTracker;
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
  }
}
