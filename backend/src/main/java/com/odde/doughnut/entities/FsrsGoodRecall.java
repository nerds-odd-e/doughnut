package com.odde.doughnut.entities;

/** FSRS-6 Good next Stability. Days in; whole hours out. Frozen default weights. */
final class FsrsGoodRecall {
  private FsrsGoodRecall() {}

  static float hoursAfterGoodRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    return Fsrs.hoursAfterShortTermOrStabilityIncrease(
        stabilityHours,
        Fsrs.GOOD,
        elapsedInHours,
        Fsrs.goodIncrementTermFromHours(stabilityHours, difficulty, elapsedInHours));
  }
}
