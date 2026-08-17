package com.odde.doughnut.entities;

/** Frozen open-FSRS-6 default weights, requested retention, Retrievability, and next Difficulty. */
final class Fsrs {
  static final double[] W = {
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835,
    0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
  };
  static final double HOURS_PER_DAY = 24.0;

  /** Global requested retention; not a Settings knob. */
  static final double REQUESTED_RETENTION = 0.9;

  static final int AGAIN = 1;
  static final int HARD = 2;
  static final int GOOD = 3;
  static final int EASY = 4;

  private Fsrs() {}

  /** Open FSRS identity: I(0.9, S) = S in whole hours. */
  static int intervalHours(float stabilityHours) {
    return Math.round(stabilityHours);
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

  /** Elapsed 0 → FSRS-6 short-term next Stability; otherwise long-term Stability increase. */
  static float hoursAfterShortTermOrStabilityIncrease(
      float stabilityHours, int grade, long elapsedInHours, double incrementTerm) {
    if (elapsedInHours == 0) {
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
    double d0Easy = W[4] - Math.exp(W[5] * (EASY - 1)) + 1.0;
    double next = W[7] * d0Easy + (1.0 - W[7]) * dPrime;
    return (float) Math.max(1.0, Math.min(10.0, next));
  }
}
