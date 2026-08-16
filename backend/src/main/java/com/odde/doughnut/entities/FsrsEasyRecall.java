package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.Fsrs.W;

/** FSRS-6 Easy next Stability and Difficulty. Days in; whole hours out. Frozen default weights. */
final class FsrsEasyRecall {
  private FsrsEasyRecall() {}

  static float hoursAfterEasyRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    return Fsrs.hoursAfterStabilityIncrease(
        stabilityHours,
        Fsrs.goodIncrementTermFromHours(stabilityHours, difficulty, elapsedInHours) * W[16]);
  }

  static float difficultyAfterEasyRecall(float difficulty) {
    return Fsrs.nextDifficulty(difficulty, Fsrs.EASY);
  }
}
