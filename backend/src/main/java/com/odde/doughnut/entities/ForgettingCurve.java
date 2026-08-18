package com.odde.doughnut.entities;

import java.util.function.Supplier;

public class ForgettingCurve {
  public static final float ASSIMILATE_STABILITY_HOURS = 0.0f;
  public static final float STRICTLY_FUTURE_FALLBACK_HOURS = 24.0f;
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
    return afterRecall(
        Fsrs.GOOD,
        () -> FsrsGoodRecall.hoursAfterGoodRecall(stabilityHours, difficulty, elapsedInHours));
  }

  NextMemory afterEasyRecall(long elapsedInHours) {
    return afterRecall(
        Fsrs.EASY,
        () -> FsrsEasyRecall.hoursAfterEasyRecall(stabilityHours, difficulty, elapsedInHours));
  }

  NextMemory afterHardRecall(long elapsedInHours) {
    return afterRecall(
        Fsrs.HARD,
        () -> FsrsHardRecall.hoursAfterHardRecall(stabilityHours, difficulty, elapsedInHours));
  }

  NextMemory afterAgainRecall(long elapsedInHours) {
    return afterRecall(
        Fsrs.AGAIN,
        () -> FsrsAgainRecall.hoursAfterAgainRecall(stabilityHours, difficulty, elapsedInHours));
  }

  private NextMemory firstRating(int grade) {
    return new NextMemory(Fsrs.initialDifficulty(grade), Fsrs.initialStabilityHours(grade));
  }

  private NextMemory afterRecall(int grade, Supplier<Float> nextStability) {
    if (isNew()) {
      return firstRating(grade);
    }
    return new NextMemory(Fsrs.nextDifficulty(difficulty, grade), nextStability.get());
  }

  public boolean isNew() {
    return stabilityHours <= ASSIMILATE_STABILITY_HOURS;
  }

  float confusionAdjusted(long elapsedInHours) {
    if (isNew()) {
      return ASSIMILATE_STABILITY_HOURS;
    }
    float againHours = afterAgainRecall(elapsedInHours).stability();
    return Math.max(1f, Math.round((stabilityHours + againHours) / 2.0f));
  }
}
