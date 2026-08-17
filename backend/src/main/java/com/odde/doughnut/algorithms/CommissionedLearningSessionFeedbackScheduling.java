package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.ProductOutcome;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static ProductOutcome productOutcomeForScore(int score) {
    return switch (score) {
      case 5 -> ProductOutcome.EASY;
      case 4 -> ProductOutcome.GOOD;
      case 3 -> ProductOutcome.HARD;
      case 2 -> ProductOutcome.SHRINK;
      case 1 -> ProductOutcome.AGAIN;
      case 0 -> ProductOutcome.AGAIN_ZERO;
      default -> throw new IllegalArgumentException("Tutor score must be 0–5: " + score);
    };
  }

  public static int scoreForProductOutcome(ProductOutcome productOutcome) {
    return switch (productOutcome) {
      case EASY -> 5;
      case GOOD -> 4;
      case HARD -> 3;
      case SHRINK -> 2;
      case AGAIN -> 1;
      case AGAIN_ZERO -> 0;
      case CONFUSION ->
          throw new IllegalArgumentException("CONFUSION is not a tutor feedback outcome");
    };
  }

  public static void recordFeedback(
      MemoryTracker tracker, Timestamp now, ProductOutcome productOutcome) {
    tracker.setRecallCount(tracker.getRecallCount() + 1);
    switch (productOutcome) {
      case EASY -> tracker.recalledEasily(now);
      case GOOD -> tracker.recalledSuccessfully(now, null);
      case HARD -> tracker.recalledHard(now);
      case SHRINK -> tracker.shrinkStability(now);
      case AGAIN, AGAIN_ZERO -> tracker.recalledAgain(now);
      case CONFUSION ->
          throw new IllegalArgumentException("CONFUSION is not a tutor feedback outcome");
    }
  }
}
