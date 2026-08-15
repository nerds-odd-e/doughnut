package com.odde.doughnut.entities;

/** FSRS-6 Good stability increment. Days in; whole hours out. Frozen default weights. */
final class FsrsStabilityIncrement {
  private static final double[] W = {
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835,
    0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
  };
  private static final double HOURS_PER_DAY = 24.0;

  private FsrsStabilityIncrement() {}

  static float hoursAfterGoodRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    double stabilityDays = stabilityHours / HOURS_PER_DAY;
    double elapsedDays = elapsedInHours / HOURS_PER_DAY;
    double retrievability = retrievability(elapsedDays, stabilityDays);
    double stabilityIncrease =
        1.0
            + Math.exp(W[8])
                * (11.0 - difficulty)
                * Math.pow(stabilityDays, -W[9])
                * (Math.exp((1.0 - retrievability) * W[10]) - 1.0);
    double nextDays = stabilityDays * Math.max(1.0, stabilityIncrease);
    return (float) Math.round(nextDays * HOURS_PER_DAY);
  }

  private static double retrievability(double elapsedDays, double stabilityDays) {
    double decay = -W[20];
    double factor = Math.exp(Math.log(0.9) / decay) - 1.0;
    return Math.pow(1.0 + factor * elapsedDays / stabilityDays, decay);
  }
}
