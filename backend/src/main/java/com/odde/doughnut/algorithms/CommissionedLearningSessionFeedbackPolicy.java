package com.odde.doughnut.algorithms;

import com.odde.doughnut.entities.ForgettingCurve;

public final class CommissionedLearningSessionFeedbackPolicy {

  private CommissionedLearningSessionFeedbackPolicy() {}

  public static float applyScore(float currentHours, int score) {
    float initial = ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
    float accumulated = Math.max(0, currentHours - initial);

    float next =
        switch (score) {
          case 2 -> initial + accumulated * 0.8f;
          case 0 -> initial;
          default -> currentHours;
        };
    return Math.max(initial, Math.round(next));
  }
}
