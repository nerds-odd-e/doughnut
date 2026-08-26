package com.odde.donut.entities;

import static com.odde.donut.entities.Fsrs.HOURS_PER_DAY;
import static com.odde.donut.entities.Fsrs.W;

/** FSRS-6 Again next Stability. Days in; whole hours out. Frozen default weights. */
final class FsrsAgainRecall {
  private FsrsAgainRecall() {}

  static float hoursAfterAgainRecall(float stabilityHours, float difficulty, long elapsedInHours) {
    return Fsrs.hoursAfterShortTermOrLongTerm(
        stabilityHours,
        Fsrs.AGAIN,
        elapsedInHours,
        hoursAfterPostLapse(stabilityHours, difficulty, elapsedInHours));
  }

  private static float hoursAfterPostLapse(
      float stabilityHours, float difficulty, long elapsedInHours) {
    double stabilityDays = stabilityHours / HOURS_PER_DAY;
    double retrievability = Fsrs.retrievabilityFromHours(stabilityHours, elapsedInHours);
    double nextDays =
        W[11]
            * Math.pow(difficulty, -W[12])
            * (Math.pow(stabilityDays + 1.0, W[13]) - 1.0)
            * Math.exp(W[14] * (1.0 - retrievability));
    return Math.min(stabilityHours, Math.max(1f, (float) Math.round(nextDays * HOURS_PER_DAY)));
  }
}
