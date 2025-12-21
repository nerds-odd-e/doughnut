package com.odde.doughnut.entities;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;

public class ForgettingCurve {
  public static final Float DEFAULT_FORGETTING_CURVE_INDEX = 100.0f;
  public static final Integer DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT = 10;
  public static final Integer BASE_THINKING_TIME_MS = 25000; // 25 seconds
  public static final Integer MAX_THINKING_TIME_MS = 60000; // 60 seconds
  private SpacedRepetitionAlgorithm spacedRepetitionAlgorithm;
  private Float forgettingCurveIndex;

  public ForgettingCurve(
      SpacedRepetitionAlgorithm spacedRepetitionAlgorithm, Float forgettingCurveIndex) {
    this.spacedRepetitionAlgorithm = spacedRepetitionAlgorithm;
    this.forgettingCurveIndex = forgettingCurveIndex;
  }

  private float add(float adjustment) {
    float newIndex = forgettingCurveIndex + adjustment;
    if (newIndex < DEFAULT_FORGETTING_CURVE_INDEX) {
      newIndex = DEFAULT_FORGETTING_CURVE_INDEX;
    }
    return newIndex;
  }

  public Integer getRepeatInHours() {
    float index =
        (float) (forgettingCurveIndex - DEFAULT_FORGETTING_CURVE_INDEX)
            / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    return spacedRepetitionAlgorithm.getRepeatInHours(index);
  }

  float succeeded(long delayInHours, Integer thinkingTimeMs) {
    float delayAdjustment = DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    Integer oldRepeatInHours = getRepeatInHours();
    if (oldRepeatInHours > 0) {
      delayAdjustment =
          DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT
              - Math.abs(delayInHours)
                  * DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT
                  / (float) oldRepeatInHours;
    }
    float thinkingTimeAdjustment = calculateThinkingTimeAdjustment(thinkingTimeMs);
    return add(delayAdjustment + thinkingTimeAdjustment);
  }

  private float calculateThinkingTimeAdjustment(Integer thinkingTimeMs) {
    if (thinkingTimeMs == null) {
      return 0.0f;
    }
    // Clamp thinking time to 0-MAX_THINKING_TIME_MS
    int clampedMs = Math.max(0, Math.min(MAX_THINKING_TIME_MS, thinkingTimeMs));
    double thinkingTimeSeconds = clampedMs / 1000.0;
    double baseThinkingTimeSeconds = BASE_THINKING_TIME_MS / 1000.0;

    // Base is BASE_THINKING_TIME_MS, adjustment = 0 at that point
    double diff = Math.abs(thinkingTimeSeconds - baseThinkingTimeSeconds);
    double adjustmentValue = Math.sqrt(diff);

    // Negative when thinking time > base thinking time
    if (thinkingTimeSeconds > baseThinkingTimeSeconds) {
      adjustmentValue = -adjustmentValue;
    }

    return (float) adjustmentValue;
  }

  public float failed() {
    return add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2);
  }
}
