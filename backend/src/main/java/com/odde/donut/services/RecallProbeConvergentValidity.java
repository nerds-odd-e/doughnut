package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.AccuracyStats;
import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Internal diagnostic (plan {@code 008-probe-convergent-analyses}) measuring how well the daily
 * probe's four readouts agree with the corresponding component of the recall history's morning
 * stats, computed the same way {@code RecallPaceAggregator}/{@code RecallAccuracyAggregator}
 * already compute those components for an arbitrary historical {@code today}. Mirrors the retired
 * split-half reliability diagnostic's own window-walk/minimum-pairs precedent (see git history for
 * {@code RecallSplitHalfReliability}, deleted in commit {@code 8ca3115dd5}), applied here to a
 * different pair of series (probe vs. recall, not odd-half vs. even-half).
 *
 * <p>Not wired into {@link com.odde.donut.controllers.dto.RecallStatsDTO} or any user-facing page.
 */
final class RecallProbeConvergentValidity {
  /**
   * How far back to look for qualifying mornings. Matches the retired split-half diagnostic's own
   * window.
   */
  private static final int TRAILING_MORNING_WINDOW_DAYS = 90;

  /**
   * Below this many day-pairs, a correlation coefficient is more noise than signal, so it is
   * reported as {@code null} rather than misleadingly precise. Matches {@code
   * RecallDayBaseline.MIN_BASELINE_DAYS} (10) and the retired split-half diagnostic's own {@code
   * MIN_PAIRS_FOR_CORRELATION} — this codebase's existing precedent for "how many days before a
   * cross-day statistic is trustworthy". Applied independently per pair below.
   */
  private static final int MIN_PAIRS_FOR_CORRELATION = 10;

  /** One of the four matched probe-metric/recall-component pairs (see plan for the mapping). */
  enum Pair {
    PACE,
    ACCURACY,
    LAPSE_COUNT,
    CONSISTENCY
  }

  /**
   * @param pairCount number of qualifying day-pairs found in the trailing window for this pair.
   */
  record PairResult(Pair pair, int pairCount, Double rawCorrelation) {}

  private RecallProbeConvergentValidity() {}

  static List<PairResult> compute(
      List<RecallAnswerRow> allTimeReviews,
      List<DailyProbe> probes,
      LocalDate today,
      ZoneId zoneId) {
    List<double[]> paceSamples = new ArrayList<>();
    List<double[]> accuracySamples = new ArrayList<>();
    List<double[]> lapseSamples = new ArrayList<>();
    List<double[]> consistencySamples = new ArrayList<>();

    for (Map.Entry<LocalDate, DailyProbe> entry :
        latestProbeByLocalDay(probes, zoneId, today).entrySet()) {
      LocalDate day = entry.getKey();
      DailyProbe probe = entry.getValue();

      RecallPaceAggregator.PaceResult paceResult =
          RecallPaceAggregator.compute(allTimeReviews, day, zoneId);
      PaceStats pace = paceResult.stats();
      if (pace.getSampleSize() == null || pace.getSampleSize() == 0) {
        // No qualifying recall data this morning: the pace/lapse/consistency readouts are either
        // null or a trivial default (e.g. lapseCount defaults to 0 with no data), not real
        // measurements to correlate against the probe.
        continue;
      }

      if (pace.getPctVsUsual() != null && probe.getSpeed() != null) {
        paceSamples.add(new double[] {pace.getPctVsUsual(), probe.getSpeed()});
      }
      if (probe.getLapseCount() != null) {
        lapseSamples.add(new double[] {pace.getLapseCount(), probe.getLapseCount()});
      }
      if (pace.getConsistencyZScore() != null && probe.getVariability() != null) {
        consistencySamples.add(new double[] {pace.getConsistencyZScore(), probe.getVariability()});
      }

      List<RecallAnswerRow> allTimeQualifyingRows =
          allTimeReviews.stream()
              .filter(r -> !paceResult.implausiblyFastRows().contains(r))
              .toList();
      List<RecallAnswerRow> todaysQualifyingRows =
          allTimeQualifyingRows.stream()
              .filter(r -> r.answerCreatedAt() != null)
              .filter(
                  r ->
                      TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId)
                          .toLocalDate()
                          .equals(day))
              .toList();
      AccuracyStats accuracy =
          RecallAccuracyAggregator.compute(
              todaysQualifyingRows, allTimeQualifyingRows, day, zoneId);
      if (accuracy.getStandardizedResidual() != null) {
        accuracySamples.add(new double[] {accuracy.getStandardizedResidual(), probe.getAccuracy()});
      }
    }

    return List.of(
        pairResult(Pair.PACE, paceSamples),
        pairResult(Pair.ACCURACY, accuracySamples),
        pairResult(Pair.LAPSE_COUNT, lapseSamples),
        pairResult(Pair.CONSISTENCY, consistencySamples));
  }

  private static PairResult pairResult(Pair pair, List<double[]> samples) {
    if (samples.size() < MIN_PAIRS_FOR_CORRELATION) {
      return new PairResult(pair, samples.size(), null);
    }
    return new PairResult(pair, samples.size(), pearson(samples));
  }

  /**
   * Latest probe per local calendar day within the trailing window, reusing {@link
   * DailyProbeDaySeries#latestByLocalDay} for the "latest completion wins" grouping and filtering
   * the result down to the window.
   */
  private static Map<LocalDate, DailyProbe> latestProbeByLocalDay(
      List<DailyProbe> probes, ZoneId zoneId, LocalDate today) {
    LocalDate windowStart = today.minusDays(TRAILING_MORNING_WINDOW_DAYS);
    Map<LocalDate, DailyProbe> latestByDay = DailyProbeDaySeries.latestByLocalDay(probes, zoneId);
    latestByDay.keySet().removeIf(day -> day.isBefore(windowStart) || day.isAfter(today));
    return latestByDay;
  }

  /**
   * Plain (population) Pearson correlation coefficient. {@code null} if either series has zero
   * variance across the sample — the coefficient is mathematically undefined (division by zero),
   * not a real zero correlation.
   */
  static Double pearson(List<double[]> pairs) {
    double meanX = pairs.stream().mapToDouble(p -> p[0]).average().orElseThrow();
    double meanY = pairs.stream().mapToDouble(p -> p[1]).average().orElseThrow();
    double num = 0;
    double denomX = 0;
    double denomY = 0;
    for (double[] p : pairs) {
      double dx = p[0] - meanX;
      double dy = p[1] - meanY;
      num += dx * dy;
      denomX += dx * dx;
      denomY += dy * dy;
    }
    if (denomX == 0 || denomY == 0) {
      return null;
    }
    return num / Math.sqrt(denomX * denomY);
  }
}
