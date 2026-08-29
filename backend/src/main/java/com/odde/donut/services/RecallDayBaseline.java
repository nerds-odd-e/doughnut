package com.odde.donut.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cross-morning median/MAD baseline for a single per-day statistic (the Pace tile's consistency
 * spread), gated by a minimum qualifying-day sample size, plus the shared z-score against it. This
 * is generic day-baseline statistics on plain {@code double} values, distinct from {@link
 * RecallPaceAggregator}'s per-item weighted-residual EWMA walk that produces the per-day values
 * feeding it.
 */
final class RecallDayBaseline {
  private static final int MIN_BASELINE_DAYS = 10;
  private static final double MAD_TO_SD_SCALE = 1.4826;

  private RecallDayBaseline() {}

  /**
   * Median and MAD of a statistic's per-day value across qualifying trailing baseline days. Both
   * fields are {@code null} when fewer than {@link #MIN_BASELINE_DAYS} days qualify.
   */
  record DayBaseline(Double median, Double mad) {}

  /**
   * Shared gating/computation for a statistic's cross-morning baseline: {@code null}/{@code null}
   * below {@link #MIN_BASELINE_DAYS} qualifying days, otherwise median/MAD of the per-day values.
   */
  static DayBaseline dayBaseline(List<Double> perDayValues) {
    if (perDayValues.size() < MIN_BASELINE_DAYS) {
      return new DayBaseline(null, null);
    }
    return new DayBaseline(median(perDayValues), mad(perDayValues));
  }

  /**
   * Z-scores a raw statistic (today's or a half's {@code pctVsUsual}/{@code lapseCount}) against an
   * existing cross-morning {@link DayBaseline}. {@code null} when the baseline isn't usable (fewer
   * than {@link #MIN_BASELINE_DAYS} qualifying days, or zero spread to standardize against).
   */
  static Double zScoreAgainstDayBaseline(double rawValue, DayBaseline baseline) {
    if (baseline.median() == null || baseline.mad() == 0) {
      return null;
    }
    return (rawValue - baseline.median()) / (baseline.mad() * MAD_TO_SD_SCALE);
  }

  /** Plain median of a list of values (average of the two middle values when the size is even). */
  static double median(List<Double> values) {
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int n = sorted.size();
    return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
  }

  /** Median absolute deviation: median distance of each value from the set's median. */
  static double mad(List<Double> values) {
    double medianValue = median(values);
    return median(values.stream().map(v -> Math.abs(v - medianValue)).toList());
  }
}
