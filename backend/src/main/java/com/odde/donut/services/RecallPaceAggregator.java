package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
import com.odde.donut.services.RecallDayBaseline.DayBaseline;
import com.odde.donut.services.RecallWeightedResidualStats.WeightedResidual;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Compares today's per-item response times against each item's own EWMA baseline to answer "am I
 * faster or slower than usual this morning". Also flags rows that are implausibly fast relative to
 * that per-item baseline (e.g. a mistap), so callers can exclude them from both pace and retention.
 */
final class RecallPaceAggregator {
  private static final double EWMA_ALPHA = 0.3;
  private static final double ABSOLUTE_FLOOR_MS = 300;
  private static final double BASELINE_FLOOR_FACTOR = 0.25;
  private static final double HARD_DROP_MS = 300_000;
  private static final double RESIDUAL_CAP = Math.log(8);
  private static final double LAPSE_FACTOR = 2.5;
  private static final int BASELINE_WINDOW_START_DAYS_AGO = 63;
  private static final int BASELINE_WINDOW_END_DAYS_AGO = 4;

  private RecallPaceAggregator() {}

  /**
   * On-task time for pace/retention purposes: {@link RecallAnswerRow#rawElapsedMs()} as-is. Unlike
   * {@link RecallStatsAggregator#responseTimeMs}, this applies no 1s drop and no 120s/300s caps —
   * this aggregator applies its own item-relative floor and its own {@link #HARD_DROP_MS} instead.
   */
  private static Optional<Long> onTaskTimeMs(RecallAnswerRow r) {
    return r.rawElapsedMs();
  }

  /** Result of the chronological pace walk: the tile stats and rows judged implausibly fast. */
  record PaceResult(PaceStats stats, Set<RecallAnswerRow> implausiblyFastRows) {}

  static PaceResult compute(List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    return compute(allTimeReviews, today, zoneId, null);
  }

  /**
   * @param todayRowsToScore restricts which of today's rows feed the today-residual/lapse-count
   *     collection step — used to score just one half (odd/even within-day sequence) of a split
   *     morning. {@code null} means every one of today's rows counts, matching the original
   *     whole-day behavior used by every other caller. The per-item EWMA baseline ({@code
   *     tauByItem}) is never restricted by this — every row, regardless of which half it falls in,
   *     still updates its item's baseline, since the baseline is built from full chronological
   *     history, not from "today" at all.
   */
  static PaceResult compute(
      List<RecallAnswerRow> allTimeReviews,
      LocalDate today,
      ZoneId zoneId,
      Set<RecallAnswerRow> todayRowsToScore) {
    Map<Integer, Double> tauByItem = new HashMap<>();
    Map<Integer, Integer> priorObservationCountByItem = new HashMap<>();
    List<WeightedResidual> todaysResiduals = new ArrayList<>();
    Map<LocalDate, List<WeightedResidual>> residualsByDate = new HashMap<>();
    Set<RecallAnswerRow> implausiblyFastRows = Collections.newSetFromMap(new IdentityHashMap<>());
    LocalDate baselineWindowStart = today.minusDays(BASELINE_WINDOW_START_DAYS_AGO);
    LocalDate baselineWindowEnd = today.minusDays(BASELINE_WINDOW_END_DAYS_AGO);
    int totalAnsweredToday = 0;
    int lapseCount = 0;
    for (RecallAnswerRow r : allTimeReviews) {
      if (r.answerCreatedAt() == null) {
        continue;
      }
      LocalDate rowDate =
          TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId).toLocalDate();
      boolean isToday = rowDate.equals(today);
      boolean scoreToday = isToday && (todayRowsToScore == null || todayRowsToScore.contains(r));
      boolean isInBaselineWindow =
          !rowDate.isBefore(baselineWindowStart) && !rowDate.isAfter(baselineWindowEnd);
      if (scoreToday) {
        totalAnsweredToday++;
      }
      Optional<Long> rt = onTaskTimeMs(r);
      if (rt.isEmpty()) {
        continue;
      }
      long onTaskMs = rt.get();
      Integer itemId = r.memoryTrackerId();
      Double baseline = tauByItem.get(itemId);
      double floorMs =
          Math.max(
              ABSOLUTE_FLOOR_MS, baseline == null ? 0 : BASELINE_FLOOR_FACTOR * Math.exp(baseline));
      if (onTaskMs < floorMs) {
        implausiblyFastRows.add(r);
        continue;
      }
      if (onTaskMs >= HARD_DROP_MS) {
        // Genuinely slow-but-valid attempts still count toward retention, but a single very
        // slow attempt on-task is dropped from the pace tile entirely and must not pollute the
        // item's baseline.
        continue;
      }
      double lnRt = Math.log(onTaskMs);
      int priorObservationCount = priorObservationCountByItem.getOrDefault(itemId, 0);
      if (baseline != null) {
        double cappedResidual = Math.min(lnRt - baseline, RESIDUAL_CAP);
        double weight = priorObservationCount / (priorObservationCount + 3.0);
        boolean isLapse = r.correct() && onTaskMs >= LAPSE_FACTOR * Math.exp(baseline);
        if (scoreToday) {
          todaysResiduals.add(new WeightedResidual(cappedResidual, weight));
          if (isLapse) {
            lapseCount++;
          }
        } else if (isInBaselineWindow) {
          residualsByDate
              .computeIfAbsent(rowDate, k -> new ArrayList<>())
              .add(new WeightedResidual(cappedResidual, weight));
        }
      }
      tauByItem.put(
          itemId, baseline == null ? lnRt : EWMA_ALPHA * lnRt + (1 - EWMA_ALPHA) * baseline);
      priorObservationCountByItem.put(itemId, priorObservationCount + 1);
    }
    int sampleSize = todaysResiduals.size();
    Double pctVsUsual =
        sampleSize > 0 ? RecallWeightedResidualStats.weightedPctVsUsual(todaysResiduals) : null;
    Double confidence =
        sampleSize > 0 ? RecallWeightedResidualStats.averageWeight(todaysResiduals) : null;
    Double consistencyZScore = consistencyZScore(todaysResiduals, residualsByDate);
    return new PaceResult(
        new PaceStats(
            pctVsUsual, sampleSize, totalAnsweredToday, confidence, lapseCount, consistencyZScore),
        implausiblyFastRows);
  }

  /**
   * Standardizes today's within-session residual spread (MAD) against the learner's own trailing
   * baseline spread (median/MAD of per-day MAD over the last 60-day window, itself excluding the
   * last 3 days). Positive means today is more erratic than usual, matching {@code pctVsUsual}'s
   * convention where positive is slower/worse. Returns {@code null} when there isn't enough data
   * (fewer than 2 residuals today, fewer than 10 qualifying baseline days, or a baseline with zero
   * spread that can't be used to standardize against).
   */
  private static Double consistencyZScore(
      List<WeightedResidual> todaysResiduals,
      Map<LocalDate, List<WeightedResidual>> residualsByDate) {
    Double todaySpread =
        todaysResiduals.size() >= 2
            ? RecallWeightedResidualStats.weightedMad(todaysResiduals)
            : null;
    List<Double> baselineSpreads =
        residualsByDate.values().stream()
            .filter(values -> values.size() >= 2)
            .map(RecallWeightedResidualStats::madOfResiduals)
            .toList();
    DayBaseline spreadBaseline = RecallDayBaseline.dayBaseline(baselineSpreads);
    return todaySpread == null
        ? null
        : RecallDayBaseline.zScoreAgainstDayBaseline(todaySpread, spreadBaseline);
  }
}
