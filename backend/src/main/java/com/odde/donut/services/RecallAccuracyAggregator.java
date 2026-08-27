package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.AccuracyStats;
import com.odde.donut.services.RecallCalibrationFitter.CalibrationFit;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardized Poisson-binomial residual comparing today's observed correctness against each
 * answer's <em>recalibrated</em> recall probability p̂ — raw FSRS retrievability (persisted on
 * {@code RecallLog.retrievability} by {@code MemoryTrackerService#persistRecallLog}) run through a
 * per-learner {@link RecallCalibrationFitter} fit on the trailing 180 days, so a scheduler that is
 * systematically over- or under-confident for this learner doesn't make the accuracy readout
 * perpetually read "worse/better than expected" for a reason that has nothing to do with today.
 * {@code A = Σ(y−p̂) / √Σp̂(1−p̂)}: positive means recalling better than the (recalibrated) model
 * expected, negative means worse.
 *
 * <p>Callers pass only rows that already qualify (answered today, counted as a review, not an
 * implausibly-fast mistap — see {@link RecallPaceAggregator}) for the numerator/denominator sum,
 * plus the all-time reviews (same exclusions applied) the calibration fit is refit from on every
 * call — there is no background job; refitting live from the already-fetched projection rows on
 * every stats request is this codebase's existing pattern for trailing-window statistics (see
 * {@code RecallPaceAggregator#consistencyZScore}'s 60-day baseline). This class applies the one
 * exclusion rule specific to the accuracy formula and the calibration fit alike: a row with no
 * persisted retrievability (e.g. a New tracker's first grade, per slice 16) is excluded — null
 * propagates, it is never treated as zero.
 */
final class RecallAccuracyAggregator {
  /** Trailing window the calibration fit is refit from on every request. */
  private static final int CALIBRATION_WINDOW_DAYS = 180;

  private RecallAccuracyAggregator() {}

  static AccuracyStats compute(
      List<RecallAnswerRow> todaysQualifyingRows,
      List<RecallAnswerRow> allTimeQualifyingRows,
      LocalDate today,
      ZoneId zoneId) {
    CalibrationFit fit =
        RecallCalibrationFitter.fit(trailingCalibrationRows(allTimeQualifyingRows, today, zoneId));

    double sumResidual = 0;
    double sumVariance = 0;
    int sampleSize = 0;
    for (RecallAnswerRow r : todaysQualifyingRows) {
      Double rawRetrievability = r.retrievability();
      if (rawRetrievability == null) {
        continue;
      }
      double p = fit.recalibrate(rawRetrievability);
      double y = r.correct() ? 1 : 0;
      sumResidual += y - p;
      sumVariance += p * (1 - p);
      sampleSize++;
    }
    Double standardizedResidual = sumVariance > 0 ? sumResidual / Math.sqrt(sumVariance) : null;
    return new AccuracyStats(standardizedResidual, sampleSize);
  }

  /**
   * Rows eligible to fit the calibration curve: non-null retrievability, strictly before today
   * (today's own outcomes must not leak into today's calibration) and within the trailing {@link
   * #CALIBRATION_WINDOW_DAYS}.
   */
  private static List<RecallAnswerRow> trailingCalibrationRows(
      List<RecallAnswerRow> allTimeQualifyingRows, LocalDate today, ZoneId zoneId) {
    LocalDate windowStart = today.minusDays(CALIBRATION_WINDOW_DAYS);
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (RecallAnswerRow r : allTimeQualifyingRows) {
      if (r.answerCreatedAt() == null || r.retrievability() == null) {
        continue;
      }
      LocalDate rowDate =
          TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId).toLocalDate();
      if (rowDate.isBefore(today) && !rowDate.isBefore(windowStart)) {
        rows.add(r);
      }
    }
    return rows;
  }
}
