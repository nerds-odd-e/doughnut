package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.Fsrs.HOURS_PER_DAY;
import static com.odde.doughnut.entities.Fsrs.W;

/** FSRS-6 Again post-lapse Stability. Days in; whole hours out. Frozen default weights. */
final class FsrsAgainRecall {
  private FsrsAgainRecall() {}

  static float hoursAfterAgainRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    double stabilityDays = stabilityHours / HOURS_PER_DAY;
    double elapsedDays = elapsedInHours / HOURS_PER_DAY;
    double retrievability = Fsrs.retrievability(elapsedDays, stabilityDays);
    double nextDays =
        W[11]
            * Math.pow(difficulty, -W[12])
            * (Math.pow(stabilityDays + 1.0, W[13]) - 1.0)
            * Math.exp(W[14] * (1.0 - retrievability));
    return (float) Math.round(nextDays * HOURS_PER_DAY);
  }
}
