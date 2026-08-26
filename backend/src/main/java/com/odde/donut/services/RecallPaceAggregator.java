package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
  private static final int MIN_BASELINE_DAYS = 10;
  private static final double MAD_TO_SD_SCALE = 1.4826;

  private RecallPaceAggregator() {}

  /** Result of the chronological pace walk: the tile stats plus rows judged implausibly fast. */
  record PaceResult(PaceStats stats, Set<RecallAnswerRow> implausiblyFastRows) {}

  /**
   * A cold-start item's baseline is built from very few observations, so its residual is noisy.
   * {@code weight} down-weights such rows in the pace tile's weighted median and confidence score.
   */
  private record WeightedResidual(double residual, double weight) {}

  static PaceResult compute(List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    Map<Integer, Double> tauByItem = new HashMap<>();
    Map<Integer, Integer> priorObservationCountByItem = new HashMap<>();
    List<WeightedResidual> todaysResiduals = new ArrayList<>();
    Map<LocalDate, List<Double>> residualsByDate = new HashMap<>();
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
      boolean isInBaselineWindow =
          !rowDate.isBefore(baselineWindowStart) && !rowDate.isAfter(baselineWindowEnd);
      if (isToday) {
        totalAnsweredToday++;
      }
      Optional<Long> rt = RecallStatsAggregator.responseTimeMs(r);
      if (rt.isEmpty()) {
        continue;
      }
      Integer itemId = r.memoryTrackerId();
      Double baseline = tauByItem.get(itemId);
      double floorMs =
          Math.max(
              ABSOLUTE_FLOOR_MS, baseline == null ? 0 : BASELINE_FLOOR_FACTOR * Math.exp(baseline));
      if (rt.get() < floorMs) {
        implausiblyFastRows.add(r);
        continue;
      }
      if (rt.get() >= HARD_DROP_MS) {
        // Genuinely slow-but-valid attempts still count toward retention, but a single very
        // slow attempt on-task is dropped from the pace tile entirely and must not pollute the
        // item's baseline.
        continue;
      }
      double lnRt = Math.log(rt.get());
      int priorObservationCount = priorObservationCountByItem.getOrDefault(itemId, 0);
      if (baseline != null) {
        double cappedResidual = Math.min(lnRt - baseline, RESIDUAL_CAP);
        if (isToday) {
          double weight = priorObservationCount / (priorObservationCount + 3.0);
          todaysResiduals.add(new WeightedResidual(cappedResidual, weight));
          if (r.correct() && rt.get() >= LAPSE_FACTOR * Math.exp(baseline)) {
            lapseCount++;
          }
        } else if (isInBaselineWindow) {
          residualsByDate.computeIfAbsent(rowDate, k -> new ArrayList<>()).add(cappedResidual);
        }
      }
      tauByItem.put(
          itemId, baseline == null ? lnRt : EWMA_ALPHA * lnRt + (1 - EWMA_ALPHA) * baseline);
      priorObservationCountByItem.put(itemId, priorObservationCount + 1);
    }
    int sampleSize = todaysResiduals.size();
    Double pctVsUsual = sampleSize > 0 ? weightedPctVsUsual(todaysResiduals) : null;
    Double confidence = sampleSize > 0 ? averageWeight(todaysResiduals) : null;
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
      List<WeightedResidual> todaysResiduals, Map<LocalDate, List<Double>> residualsByDate) {
    List<Double> todaysPlainResiduals =
        todaysResiduals.stream().map(WeightedResidual::residual).toList();
    Double todaySpread = todaysPlainResiduals.size() >= 2 ? mad(todaysPlainResiduals) : null;
    List<Double> baselineSpreads =
        residualsByDate.values().stream()
            .filter(values -> values.size() >= 2)
            .map(RecallPaceAggregator::mad)
            .toList();
    if (todaySpread == null || baselineSpreads.size() < MIN_BASELINE_DAYS) {
      return null;
    }
    double baselineMedian = median(baselineSpreads);
    double baselineMad = mad(baselineSpreads);
    if (baselineMad == 0) {
      return null;
    }
    return (todaySpread - baselineMedian) / (baselineMad * MAD_TO_SD_SCALE);
  }

  /** Plain median of a list of values (average of the two middle values when the size is even). */
  private static double median(List<Double> values) {
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int n = sorted.size();
    return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
  }

  /** Median absolute deviation: median distance of each value from the set's median. */
  private static double mad(List<Double> values) {
    double medianValue = median(values);
    return median(values.stream().map(v -> Math.abs(v - medianValue)).toList());
  }

  private static Double weightedPctVsUsual(List<WeightedResidual> residuals) {
    Double weightedMedian = weightedMedian(residuals);
    return weightedMedian == null ? null : (Math.exp(weightedMedian) - 1) * 100;
  }

  private static double averageWeight(List<WeightedResidual> residuals) {
    return residuals.stream().mapToDouble(WeightedResidual::weight).average().orElse(0);
  }

  /**
   * Weighted median: sorts by residual ascending and returns the residual at which cumulative
   * weight first reaches half of the total. Cold-start rows (weight near 0) barely move it, letting
   * established items dominate. Returns {@code null} when total weight is zero (every contributing
   * row is cold-start), since such weights carry no information either way.
   */
  private static Double weightedMedian(List<WeightedResidual> residuals) {
    double totalWeight = residuals.stream().mapToDouble(WeightedResidual::weight).sum();
    if (totalWeight == 0) {
      return null;
    }
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
