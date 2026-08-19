package com.odde.doughnut.entities;

import java.util.function.BiFunction;

/**
 * Frozen open-FSRS-6 default weights, requested retention, maximum interval, Retrievability, next
 * Difficulty, New vs graded first-rating, and DSR constants.
 */
public final class Fsrs {
  public static final float NEW_STABILITY_HOURS = 0.0f;
  public static final float STRICTLY_FUTURE_FALLBACK_HOURS = 24.0f;
  public static final float DEFAULT_DIFFICULTY = 5.0f;

  static final double[] W = {
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835,
    0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
  };
  static final double HOURS_PER_DAY = 24.0;

  /** Global requested retention; not a Settings knob. */
  static final double REQUESTED_RETENTION = 0.9;

  /** Global maximum interval in whole hours; not a Settings knob. */
  static final float MAXIMUM_INTERVAL_HOURS = 876000f;

  static final int AGAIN = 1;
  static final int HARD = 2;
  static final int GOOD = 3;
  static final int EASY = 4;

  private Fsrs() {}

  record NextMemory(float difficulty, float stability) {}

  public static boolean isNew(float stabilityHours) {
    return stabilityHours <= NEW_STABILITY_HOURS;
  }

  static NextMemory firstRating(int grade) {
    return new NextMemory(initialDifficulty(grade), initialStabilityHours(grade));
  }

  static NextMemory afterGoodRecall(float stabilityHours, Float difficulty, long elapsedInHours) {
    return afterRecall(
        stabilityHours,
        difficulty,
        GOOD,
        (s, d) -> FsrsGoodRecall.hoursAfterGoodRecall(s, d, elapsedInHours));
  }

  static NextMemory afterEasyRecall(float stabilityHours, Float difficulty, long elapsedInHours) {
    return afterRecall(
        stabilityHours,
        difficulty,
        EASY,
        (s, d) -> FsrsEasyRecall.hoursAfterEasyRecall(s, d, elapsedInHours));
  }

  static NextMemory afterHardRecall(float stabilityHours, Float difficulty, long elapsedInHours) {
    return afterRecall(
        stabilityHours,
        difficulty,
        HARD,
        (s, d) -> FsrsHardRecall.hoursAfterHardRecall(s, d, elapsedInHours));
  }

  static NextMemory afterAgainRecall(float stabilityHours, Float difficulty, long elapsedInHours) {
    return afterRecall(
        stabilityHours,
        difficulty,
        AGAIN,
        (s, d) -> FsrsAgainRecall.hoursAfterAgainRecall(s, d, elapsedInHours));
  }

  static float confusionAdjusted(float stabilityHours, Float difficulty, long elapsedInHours) {
    float s = clampedStability(stabilityHours);
    if (isNew(s)) {
      return NEW_STABILITY_HOURS;
    }
    float againHours = afterAgainRecall(s, difficulty, elapsedInHours).stability();
    return Math.max(1f, Math.round((s + againHours) / 2.0f));
  }

  /** Open FSRS identity: I(0.9, S) = S in whole hours. */
  static int intervalHours(float stabilityHours) {
    return Math.round(stabilityHours);
  }

  static float cappedStabilityHours(float stabilityHours) {
    return Math.min(stabilityHours, MAXIMUM_INTERVAL_HOURS);
  }

  static double retrievability(double elapsedDays, double stabilityDays) {
    double decay = -W[20];
    double factor = Math.exp(Math.log(REQUESTED_RETENTION) / decay) - 1.0;
    return Math.pow(1.0 + factor * elapsedDays / stabilityDays, decay);
  }

  static double retrievabilityFromHours(float stabilityHours, long elapsedInHours) {
    return retrievability(elapsedInHours / HOURS_PER_DAY, stabilityHours / HOURS_PER_DAY);
  }

  static double goodIncrementTerm(double stabilityDays, double difficulty, double retrievability) {
    return Math.exp(W[8])
        * (11.0 - difficulty)
        * Math.pow(stabilityDays, -W[9])
        * (Math.exp((1.0 - retrievability) * W[10]) - 1.0);
  }

  static double goodIncrementTermFromHours(
      float stabilityHours, float difficulty, long elapsedInHours) {
    return goodIncrementTerm(
        stabilityHours / HOURS_PER_DAY,
        difficulty,
        retrievabilityFromHours(stabilityHours, elapsedInHours));
  }

  static float hoursAfterStabilityIncrease(float stabilityHours, double incrementTerm) {
    double nextDays = (stabilityHours / HOURS_PER_DAY) * Math.max(1.0, 1.0 + incrementTerm);
    return (float) Math.round(nextDays * HOURS_PER_DAY);
  }

  /**
   * Elapsed hours under 24 use FSRS-6 short-term next Stability; otherwise long-term Stability
   * increase.
   */
  static float hoursAfterShortTermOrStabilityIncrease(
      float stabilityHours, int grade, long elapsedInHours, double incrementTerm) {
    if (elapsedInHours < HOURS_PER_DAY) {
      return hoursAfterShortTermRecall(stabilityHours, grade);
    }
    return hoursAfterStabilityIncrease(stabilityHours, incrementTerm);
  }

  /** Published FSRS-6 short-term next Stability. Clamp SInc ≥ 1 so S does not shrink. */
  static float hoursAfterShortTermRecall(float stabilityHours, int grade) {
    double stabilityDays = stabilityHours / HOURS_PER_DAY;
    double sInc = Math.exp(W[17] * (grade - GOOD + W[18])) * Math.pow(stabilityDays, -W[19]);
    double nextDays = stabilityDays * Math.max(1.0, sInc);
    return (float) Math.round(nextDays * HOURS_PER_DAY);
  }

  static float nextDifficulty(float difficulty, int grade) {
    double deltaD = -W[6] * (grade - GOOD);
    double dPrime = difficulty + deltaD * (10.0 - difficulty) / 9.0;
    return clampDifficulty(W[7] * d0(EASY) + (1.0 - W[7]) * dPrime);
  }

  static float initialDifficulty(int grade) {
    return clampDifficulty(d0(grade));
  }

  static float initialStabilityHours(int grade) {
    return (float) Math.round(W[grade - 1] * HOURS_PER_DAY);
  }

  private static NextMemory afterRecall(
      float stabilityHours,
      Float difficulty,
      int grade,
      BiFunction<Float, Float, Float> nextStability) {
    float s = clampedStability(stabilityHours);
    float d = difficultyOrDefault(difficulty);
    if (isNew(s)) {
      return firstRating(grade);
    }
    return new NextMemory(nextDifficulty(d, grade), nextStability.apply(s, d));
  }

  private static float clampedStability(float stabilityHours) {
    return Math.max(NEW_STABILITY_HOURS, stabilityHours);
  }

  private static float difficultyOrDefault(Float difficulty) {
    return difficulty == null ? DEFAULT_DIFFICULTY : difficulty;
  }

  private static double d0(int grade) {
    return W[4] - Math.exp(W[5] * (grade - 1)) + 1.0;
  }

  private static float clampDifficulty(double difficulty) {
    return (float) Math.max(1.0, Math.min(10.0, difficulty));
  }
}
