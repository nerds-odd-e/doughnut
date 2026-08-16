package com.odde.doughnut.entities;

/** Frozen open-FSRS-6 default weights and Retrievability. */
final class Fsrs {
  static final double[] W = {
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835,
    0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
  };
  static final double HOURS_PER_DAY = 24.0;

  private Fsrs() {}

  static double retrievability(double elapsedDays, double stabilityDays) {
    double decay = -W[20];
    double factor = Math.exp(Math.log(0.9) / decay) - 1.0;
    return Math.pow(1.0 + factor * elapsedDays / stabilityDays, decay);
  }
}
