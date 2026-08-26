package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compares today's per-item response times against each item's own EWMA baseline to answer "am I
 * faster or slower than usual this morning".
 */
final class RecallPaceAggregator {
  private static final double EWMA_ALPHA = 0.3;

  private RecallPaceAggregator() {}

  static PaceStats buildPace(List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    Map<Integer, Double> tauByItem = new HashMap<>();
    List<Double> todaysResiduals = new ArrayList<>();
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
      double lnRt = Math.log(rt.get());
      Integer itemId = r.memoryTrackerId();
      Double baseline = tauByItem.get(itemId);
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
    return new PaceStats(pctVsUsual, sampleSize, totalAnsweredToday);
  }
}
