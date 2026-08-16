package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.Fsrs.HOURS_PER_DAY;
import static com.odde.doughnut.entities.Fsrs.W;

/** FSRS-6 Good next Stability and Difficulty. Days in; whole hours out. Frozen default weights. */
final class FsrsGoodRecall {
  private FsrsGoodRecall() {}

  static float hoursAfterGoodRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    double stabilityDays = stabilityHours / HOURS_PER_DAY;
    double elapsedDays = elapsedInHours / HOURS_PER_DAY;
    double retrievability = Fsrs.retrievability(elapsedDays, stabilityDays);
    double stabilityIncrease =
        1.0
            + Math.exp(W[8])
                * (11.0 - difficulty)
                * Math.pow(stabilityDays, -W[9])
                * (Math.exp((1.0 - retrievability) * W[10]) - 1.0);
    double nextDays = stabilityDays * Math.max(1.0, stabilityIncrease);
    return (float) Math.round(nextDays * HOURS_PER_DAY);
  }

  static float difficultyAfterGoodRecall(float difficulty) {
    final int good = 3;
    double deltaD = -W[6] * (good - 3);
    double next = W[7] * W[4] + (1.0 - W[7]) * (difficulty + deltaD);
    return (float) Math.max(1.0, Math.min(10.0, next));
  }
}
