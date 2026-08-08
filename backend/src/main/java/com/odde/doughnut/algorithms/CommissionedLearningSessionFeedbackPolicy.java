package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.ForgettingCurve;

public final class CommissionedLearningSessionFeedbackPolicy {

  private CommissionedLearningSessionFeedbackPolicy() {}

  public static float applyScore(float currentIndex, int score) {
    float initial = ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;
    float accumulated = Math.max(0, currentIndex - initial);
    float standardIncrement = ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;

    return switch (score) {
      case 5 -> currentIndex + standardIncrement * 1.2f;
      case 4 -> currentIndex + standardIncrement;
      case 3 -> currentIndex + standardIncrement * 0.8f;
      case 2 -> initial + accumulated * 0.8f;
      case 1 -> initial + accumulated * 0.5f;
      case 0 -> initial;
      default -> currentIndex;
    };
  }
}
