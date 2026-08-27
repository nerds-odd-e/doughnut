package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.AccuracyStats;
import java.util.List;

/**
 * Standardized Poisson-binomial residual comparing today's observed correctness against each
 * answer's raw FSRS retrievability at the time it was answered — the value already persisted on
 * {@code RecallLog.retrievability} by {@code MemoryTrackerService#persistRecallLog}, not one
 * recomputed here. {@code A = Σ(y−p̂) / √Σp̂(1−p̂)}: positive means recalling better than the model
 * expected, negative means worse.
 *
 * <p>Callers pass only rows that already qualify (answered today, counted as a review, not an
 * implausibly-fast mistap — see {@link RecallPaceAggregator}); this class applies just the one
 * exclusion rule specific to the accuracy formula: a row with no persisted retrievability (e.g. a
 * New tracker's first grade, per slice 16) is excluded from the sum — null propagates, it is never
 * treated as zero.
 */
final class RecallAccuracyAggregator {
  private RecallAccuracyAggregator() {}

  static AccuracyStats compute(List<RecallAnswerRow> todaysQualifyingRows) {
    double sumResidual = 0;
    double sumVariance = 0;
    int sampleSize = 0;
    for (RecallAnswerRow r : todaysQualifyingRows) {
      Double p = r.retrievability();
      if (p == null) {
        continue;
      }
      double y = r.correct() ? 1 : 0;
      sumResidual += y - p;
      sumVariance += p * (1 - p);
      sampleSize++;
    }
    Double standardizedResidual = sumVariance > 0 ? sumResidual / Math.sqrt(sumVariance) : null;
    return new AccuracyStats(standardizedResidual, sampleSize);
  }
}
