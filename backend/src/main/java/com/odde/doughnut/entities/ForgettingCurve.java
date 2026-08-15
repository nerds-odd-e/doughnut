package com.odde.doughnut.entities;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;

public class ForgettingCurve {
  public static final float ASSIMILATE_STABILITY_HOURS = 0.0f;
  public static final Integer BASE_THINKING_TIME_MS = 25000; // 25 seconds
  public static final Integer MAX_THINKING_TIME_MS = 60000; // 60 seconds
  private final float stabilityHours;

  public ForgettingCurve(float stabilityHours) {
    this.stabilityHours = Math.max(ASSIMILATE_STABILITY_HOURS, stabilityHours);
  }

  float succeeded(long elapsedInHours, Integer thinkingTimeMs) {
    float successIncrement = 1.0f;
    if (stabilityHours > 0 && elapsedInHours < stabilityHours) {
      successIncrement += (elapsedInHours - stabilityHours) / stabilityHours;
    }
    float thinkingTimeAdjustment = calculateThinkingTimeAdjustment(thinkingTimeMs);
    return (float)
        SpacedRepetitionAlgorithm.hoursAfterSpacingDelta(
            stabilityHours, successIncrement + thinkingTimeAdjustment, true);
  }

  private float calculateThinkingTimeAdjustment(Integer thinkingTimeMs) {
    if (thinkingTimeMs == null) {
      return 0.0f;
    }
    int clampedMs = Math.max(0, Math.min(MAX_THINKING_TIME_MS, thinkingTimeMs));
    double thinkingTimeSeconds = clampedMs / 1000.0;
    double baseThinkingTimeSeconds = BASE_THINKING_TIME_MS / 1000.0;

    double diff = Math.abs(thinkingTimeSeconds - baseThinkingTimeSeconds);
    double adjustmentValue = Math.sqrt(diff) / SpacedRepetitionAlgorithm.LEGACY_INDEX_STEP;

    if (thinkingTimeSeconds > baseThinkingTimeSeconds) {
      adjustmentValue = -adjustmentValue;
    }

    return (float) adjustmentValue;
  }

  public float failed() {
    return (float) SpacedRepetitionAlgorithm.hoursAfterSpacingDelta(stabilityHours, -2, false);
  }

  public float confusionAdjusted() {
    return (float) SpacedRepetitionAlgorithm.hoursAfterSpacingDelta(stabilityHours, -1, false);
  }
}
