package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.Fsrs.W;

/** FSRS-6 Hard next Stability and Difficulty. Days in; whole hours out. Frozen default weights. */
final class FsrsHardRecall {
  private FsrsHardRecall() {}

  static float hoursAfterHardRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    return Fsrs.hoursAfterStabilityIncrease(
        stabilityHours,
        Fsrs.goodIncrementTermFromHours(stabilityHours, difficulty, elapsedInHours) * W[15]);
  }

  static float difficultyAfterHardRecall(float difficulty) {
    return Fsrs.nextDifficulty(difficulty, Fsrs.HARD);
  }
}
