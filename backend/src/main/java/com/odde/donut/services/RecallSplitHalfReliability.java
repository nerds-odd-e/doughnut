package com.odde.donut.services;

import com.odde.donut.services.RecallMorningHalfIndex.HalfIndexes;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal diagnostic (slice 21.4) for the morning cognitive index's split-half reliability: across
 * a trailing window of historical mornings, scores each qualifying day's odd- and even-indexed
 * attempts ({@link RecallMorningHalfIndex#computeBothHalves}) and reports how well the two halves
 * agree. This is the reliability gate the plan's "The index" section requires before slices 22-25
 * (the user-facing composite) can ship — see the Jidoka checkpoint before slice 22.
 *
 * <p>Not wired into {@link com.odde.donut.controllers.dto.RecallStatsDTO} or any user-facing page.
 */
final class RecallSplitHalfReliability {
  // TEMP-DEBUG (slice 21.4 prod investigation, remove before merge stays):
  private static final Logger TEMP_DEBUG_LOG =
      LoggerFactory.getLogger(RecallSplitHalfReliability.class);

  /**
   * How far back to look for qualifying mornings. 90 days keeps the estimate about *recent*
   * reliability (consistent with the ~60-day windows the pace/consistency baselines already use)
   * while giving a real chance at the {@link #MIN_PAIRS_FOR_CORRELATION} floor below.
   */
  private static final int TRAILING_MORNING_WINDOW_DAYS = 90;

  /**
   * A day is a *candidate* for splitting only if it has at least this many qualifying rows overall.
   * Mirrors {@code RecallPaceAggregator.consistencyZScore}'s own minimum of >= 2 residuals for a
   * half to produce a non-null consistency z-score: 4 total rows is the fewest that can put >= 2 in
   * each of two non-empty halves. This is a coarse, cheap pre-filter only — the real gate is still
   * "did {@link RecallMorningHalfIndex#computeBothHalves} return non-null for both halves" (a day
   * can pass this row-count filter and still be excluded, e.g. if some rows lack a warmed-up
   * per-item baseline).
   */
  private static final int MIN_QUALIFYING_ROWS_PER_DAY = 4;

  /**
   * Below this many day-pairs, a correlation coefficient is more noise than signal, so both
   * reported numbers are {@code null} rather than misleadingly precise. Chosen to match {@code
   * RecallDayBaseline.MIN_BASELINE_DAYS} (10) — this codebase's existing precedent for "how many
   * days before a cross-day statistic is trustworthy.
   */
  private static final int MIN_PAIRS_FOR_CORRELATION = 10;

  private RecallSplitHalfReliability() {}

  /**
   * @param pairCount number of qualifying day-pairs (both halves non-null) found in the trailing
   *     window.
   * @param rawCorrelation Pearson correlation between odd-half and even-half index values across
   *     those pairs; {@code null} if fewer than {@link #MIN_PAIRS_FOR_CORRELATION} pairs exist, or
   *     if either half's values have zero variance across the sample (correlation undefined).
   * @param spearmanBrownCorrelation the Spearman-Brown-corrected estimate {@code 2r / (1+r)} of the
   *     full-morning (not half-morning) reliability; {@code null} under the same conditions as
   *     {@code rawCorrelation}.
   */
  record Result(int pairCount, Double rawCorrelation, Double spearmanBrownCorrelation) {}

  static Result compute(List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    List<LocalDate> candidates = candidateDays(allTimeReviews, today, zoneId);
    List<double[]> pairs = new ArrayList<>();
    for (LocalDate day : candidates) {
      HalfIndexes halves = RecallMorningHalfIndex.computeBothHalves(allTimeReviews, day, zoneId);
      if (halves.odd() != null && halves.even() != null) {
        pairs.add(new double[] {halves.odd(), halves.even()});
      }
    }
    TEMP_DEBUG_LOG.warn(
        "TEMP-DEBUG splitHalf summary allTimeReviews={} candidateDays={} pairs={}",
        allTimeReviews.size(),
        candidates.size(),
        pairs.size());
    if (pairs.size() < MIN_PAIRS_FOR_CORRELATION) {
      return new Result(pairs.size(), null, null);
    }
    Double r = pearson(pairs);
    Double correctedR = r == null ? null : (2 * r) / (1 + r);
    return new Result(pairs.size(), r, correctedR);
  }

  /**
   * Distinct calendar days within the trailing window that have at least {@link
   * #MIN_QUALIFYING_ROWS_PER_DAY} rows, in ascending order. A coarse pre-filter over raw row counts
   * (not accounting for implausibly-fast rows or per-item baseline warm-up) purely so {@link
   * #compute} doesn't run the full half-index pipeline against every historical day; the real
   * qualification is {@link RecallMorningHalfIndex#computeBothHalves} returning non-null for both
   * halves.
   */
  private static List<LocalDate> candidateDays(
      List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    LocalDate windowStart = today.minusDays(TRAILING_MORNING_WINDOW_DAYS);
    Map<LocalDate, Integer> countByDay = new HashMap<>();
    for (RecallAnswerRow r : allTimeReviews) {
      if (r.answerCreatedAt() == null) {
        continue;
      }
      LocalDate day =
          TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId).toLocalDate();
      if (day.isBefore(windowStart) || day.isAfter(today)) {
        continue;
      }
      countByDay.merge(day, 1, Integer::sum);
    }
    return countByDay.entrySet().stream()
        .filter(e -> e.getValue() >= MIN_QUALIFYING_ROWS_PER_DAY)
        .map(Map.Entry::getKey)
        .sorted()
        .toList();
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
