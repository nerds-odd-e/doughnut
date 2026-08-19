package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.ProductOutcome;
import java.sql.Timestamp;

public final class CommissionedLearningSessionFeedbackScheduling {

  private CommissionedLearningSessionFeedbackScheduling() {}

  public static ProductOutcome productOutcomeForScore(int score) {
    return switch (score) {
      case 4 -> ProductOutcome.EASY;
      case 3 -> ProductOutcome.GOOD;
      case 2 -> ProductOutcome.HARD;
      case 1 -> ProductOutcome.AGAIN;
      default -> throw new IllegalArgumentException("Tutor score must be 1–4: " + score);
    };
  }

  public static int scoreForProductOutcome(ProductOutcome productOutcome) {
    return switch (productOutcome) {
      case EASY -> 4;
      case GOOD -> 3;
      case HARD -> 2;
      case AGAIN -> 1;
      case CONFUSION ->
          throw new IllegalArgumentException("CONFUSION is not a tutor feedback outcome");
    };
  }

  public static void recordFeedback(
      MemoryTracker tracker, Timestamp now, ProductOutcome productOutcome) {
    switch (productOutcome) {
      case EASY -> tracker.recalledEasily(now);
      case GOOD -> tracker.recalledSuccessfully(now);
      case HARD -> tracker.recalledHard(now);
      case AGAIN -> tracker.recalledAgain(now);
      case CONFUSION ->
          throw new IllegalArgumentException("CONFUSION is not a tutor feedback outcome");
    }
  }
}
