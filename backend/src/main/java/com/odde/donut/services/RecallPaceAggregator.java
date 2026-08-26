package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
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

  private RecallPaceAggregator() {}

  /** Result of the chronological pace walk: the tile stats plus rows judged implausibly fast. */
  record PaceResult(PaceStats stats, Set<RecallAnswerRow> implausiblyFastRows) {}

  static PaceResult compute(List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    Map<Integer, Double> tauByItem = new HashMap<>();
    List<Double> todaysResiduals = new ArrayList<>();
    Set<RecallAnswerRow> implausiblyFastRows = Collections.newSetFromMap(new IdentityHashMap<>());
    int totalAnsweredToday = 0;
    for (RecallAnswerRow r : allTimeReviews) {
      if (r.answerCreatedAt() == null) {
        continue;
      }
      boolean isToday =
          TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId)
              .toLocalDate()
              .equals(today);
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
      double lnRt = Math.log(rt.get());
      if (baseline != null && isToday) {
        todaysResiduals.add(lnRt - baseline);
      }
      tauByItem.put(
          itemId, baseline == null ? lnRt : EWMA_ALPHA * lnRt + (1 - EWMA_ALPHA) * baseline);
    }
    int sampleSize = todaysResiduals.size();
    Double pctVsUsual = null;
    if (sampleSize > 0) {
      double sumResiduals = 0;
      for (double residual : todaysResiduals) {
        sumResiduals += residual;
      }
      pctVsUsual = (Math.exp(sumResiduals / sampleSize) - 1) * 100;
    }
    return new PaceResult(
        new PaceStats(pctVsUsual, sampleSize, totalAnsweredToday), implausiblyFastRows);
  }
}
