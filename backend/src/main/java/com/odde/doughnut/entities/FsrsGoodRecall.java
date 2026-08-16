package com.odde.doughnut.entities;

/** FSRS-6 Good next Stability and Difficulty. Days in; whole hours out. Frozen default weights. */
final class FsrsGoodRecall {
  private FsrsGoodRecall() {}

  static float hoursAfterGoodRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    return Fsrs.hoursAfterStabilityIncrease(
        stabilityHours,
        Fsrs.goodIncrementTermFromHours(stabilityHours, difficulty, elapsedInHours));
  }

  static float difficultyAfterGoodRecall(float difficulty) {
    return Fsrs.nextDifficulty(difficulty, Fsrs.GOOD);
  }
}
