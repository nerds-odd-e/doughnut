package com.odde.donut.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Weighted-median-based statistics over per-item response-time residuals, where a residual's {@code
 * weight} down-weights cold-start items (few prior observations) so a morning of noisy new cards
 * can't dominate the weighted median or its spread. Extracted from {@link RecallPaceAggregator} to
 * keep that file under the 250-line convention; this is pure statistics over already-computed
 * residuals, distinct from the chronological EWMA baseline walk that produces them.
 */
final class RecallWeightedResidualStats {
  /**
   * A cold-start item's baseline is built from very few observations, so its residual is noisy.
   * {@code weight} down-weights such rows in the weighted median and confidence score.
   */
  record WeightedResidual(double residual, double weight) {}

  private RecallWeightedResidualStats() {}

  static double weightedPctVsUsual(List<WeightedResidual> residuals) {
    return (Math.exp(weightedMedian(residuals)) - 1) * 100;
  }

  static double averageWeight(List<WeightedResidual> residuals) {
    return residuals.stream().mapToDouble(WeightedResidual::weight).average().orElse(0);
  }

  /**
   * Plain (unweighted) MAD of a baseline day's residuals — deliberately ignores each residual's
   * cold-start weight, unlike {@link #weightedMad} used for today's spread.
   */
  static double madOfResiduals(List<WeightedResidual> residuals) {
    return RecallDayBaseline.mad(residuals.stream().map(WeightedResidual::residual).toList());
  }

  /**
   * Weighted median absolute deviation: same cold-start down-weighting as {@link
   * #weightedPctVsUsual}, applied to today's spread so a morning of noisy new cards can't flip the
   * consistency badge when a well-established item's residual is tight.
   */
  static double weightedMad(List<WeightedResidual> residuals) {
    double weightedMedianValue = weightedMedian(residuals);
    List<WeightedResidual> deviations =
        residuals.stream()
            .map(
                wr ->
                    new WeightedResidual(
                        Math.abs(wr.residual() - weightedMedianValue), wr.weight()))
            .toList();
    return weightedMedian(deviations);
  }

  /**
   * Weighted median: sorts by residual ascending and returns the residual at which cumulative
   * weight first reaches half of the total. Cold-start rows (weight as low as 0.25, the minimum a
   * residual-producing row can carry) barely move it, letting established items dominate.
   */
  static double weightedMedian(List<WeightedResidual> residuals) {
    double totalWeight = residuals.stream().mapToDouble(WeightedResidual::weight).sum();
    List<WeightedResidual> sorted = new ArrayList<>(residuals);
    sorted.sort(Comparator.comparingDouble(WeightedResidual::residual));
    double cumulativeWeight = 0;
    for (WeightedResidual wr : sorted) {
      cumulativeWeight += wr.weight();
      if (cumulativeWeight >= totalWeight / 2.0) {
        return wr.residual();
      }
    }
    return sorted.get(sorted.size() - 1).residual();
  }
}
