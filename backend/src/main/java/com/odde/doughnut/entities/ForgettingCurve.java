package com.odde.doughnut.entities;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import java.util.function.Supplier;

public class ForgettingCurve {
  public static final float ASSIMILATE_STABILITY_HOURS = 0.0f;
  public static final float FIRST_SUCCESS_STABILITY_HOURS = 24.0f;
  public static final float DEFAULT_DIFFICULTY = 5.0f;
  public static final Integer BASE_THINKING_TIME_MS = 25000; // 25 seconds
  public static final Integer MAX_THINKING_TIME_MS = 60000; // 60 seconds
  private final float stabilityHours;
  private final float difficulty;

  public ForgettingCurve(float stabilityHours) {
    this(stabilityHours, null);
  }

  public ForgettingCurve(float stabilityHours, Float difficulty) {
    this.stabilityHours = Math.max(ASSIMILATE_STABILITY_HOURS, stabilityHours);
    this.difficulty = difficulty == null ? DEFAULT_DIFFICULTY : difficulty;
  }

  record NextMemory(float difficulty, float stability) {}

  NextMemory afterGoodRecall(long elapsedInHours, Integer thinkingTimeMs) {
    return afterGoodHardOrEasyRecall(
        () -> Fsrs.nextDifficulty(difficulty, Fsrs.GOOD),
        () ->
            adjustForThinkingTime(
                FsrsGoodRecall.hoursAfterGoodRecall(stabilityHours, difficulty, elapsedInHours),
                thinkingTimeMs));
  }

  NextMemory afterEasyRecall(long elapsedInHours) {
    return afterGoodHardOrEasyRecall(
        () -> Fsrs.nextDifficulty(difficulty, Fsrs.EASY),
        () -> FsrsEasyRecall.hoursAfterEasyRecall(stabilityHours, difficulty, elapsedInHours));
  }

  NextMemory afterHardRecall(long elapsedInHours) {
    return afterGoodHardOrEasyRecall(
        () -> Fsrs.nextDifficulty(difficulty, Fsrs.HARD),
        () -> FsrsHardRecall.hoursAfterHardRecall(stabilityHours, difficulty, elapsedInHours));
  }

  float succeeded(long elapsedInHours, Integer thinkingTimeMs) {
    return afterGoodRecall(elapsedInHours, thinkingTimeMs).stability();
  }

  private NextMemory afterGoodHardOrEasyRecall(
      Supplier<Float> nextDifficulty, Supplier<Float> nextStability) {
    if (isNewlyAssimilated()) {
      return new NextMemory(DEFAULT_DIFFICULTY, FIRST_SUCCESS_STABILITY_HOURS);
    }
    return new NextMemory(nextDifficulty.get(), nextStability.get());
  }

  float difficultyAfterFailedRecall() {
    return Fsrs.nextDifficulty(difficulty, Fsrs.AGAIN);
  }

  boolean isNewlyAssimilated() {
    return stabilityHours <= ASSIMILATE_STABILITY_HOURS;
  }

  private float adjustForThinkingTime(float fsrsHours, Integer thinkingTimeMs) {
    float increment = fsrsHours - stabilityHours;
    float adjustment = calculateThinkingTimeAdjustment(thinkingTimeMs);
    float tweakBase = increment > 0 ? increment : stabilityHours;
    float tweaked = fsrsHours + tweakBase * adjustment;
    return Math.max(stabilityHours, Math.round(tweaked));
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

  public float failed(long elapsedInHours) {
    if (isNewlyAssimilated()) {
      return ASSIMILATE_STABILITY_HOURS;
    }
    return FsrsAgainRecall.hoursAfterAgainRecall(stabilityHours, difficulty, elapsedInHours);
  }

  float confusionAdjusted(long elapsedInHours) {
    float againHours = failed(elapsedInHours);
    if (isNewlyAssimilated()) {
      return againHours;
    }
    return Math.max(1f, Math.round((stabilityHours + againHours) / 2.0f));
  }
}
