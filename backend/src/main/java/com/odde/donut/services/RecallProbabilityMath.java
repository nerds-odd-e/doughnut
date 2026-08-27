package com.odde.donut.services;

/**
 * Logistic-model probability primitives shared by {@link RecallCalibrationFitter} (2PL) and {@link
 * RecallGuessingFloorFitter} (3PL): both fit a logistic curve over {@code (logit(retrievability),
 * outcome)} pairs and need the same numerically-safe clamp/logit/sigmoid to do it — the two fits
 * differ in how many parameters they solve for and how the Hessian is approximated, not in these
 * primitives.
 */
final class RecallProbabilityMath {
  private static final double PROBABILITY_EPSILON = 1e-6;

  private RecallProbabilityMath() {}

  /**
   * Clamps a probability away from exactly 0 or 1, where {@link #logit} and log-likelihood blow up.
   */
  static double clamp(double p) {
    return Math.min(1 - PROBABILITY_EPSILON, Math.max(PROBABILITY_EPSILON, p));
  }

  static double logit(double p) {
    return Math.log(p / (1 - p));
  }

  static double sigmoid(double z) {
    return 1.0 / (1.0 + Math.exp(-z));
  }
}
