package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO.AccuracyStats;
import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
import com.odde.donut.entities.QuestionType;
import com.odde.donut.services.RecallGuessingFloorFitter.ThreePlFit;
import com.odde.donut.services.RecallPaceAggregator.PaceResult;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scores odd- or even-indexed halves (1-indexed by within-day chronological order) of a single
 * historical day D's recall attempts through the same accuracy/pace/lapse/consistency machinery
 * already used to score a whole day (slices 17-20, 21.1) — producing one composite index value
 * (slice 21.2's formula) per half. Enables slice 21.4's split-half reliability check.
 *
 * <p>Trailing-window calibration/baselines are never recomputed per half: they are derived from
 * history strictly before D (the accuracy calibration's trailing-180-day window excludes today by
 * construction, and the pace/lapse/consistency baseline window already ends {@code
 * BASELINE_WINDOW_END_DAYS_AGO} days before D regardless of which of D's own rows are being scored)
 * — restricting to a half only changes which of D's own rows feed the numerator, never what they
 * are compared against. {@link #computeBothHalves} therefore prepares that shared day-level setup
 * once (whole-day pace exclusions, qualifying-row order, and the per-question-type 3PL fit) and
 * applies it to each half.
 */
final class RecallMorningHalfIndex {
  // TEMP-DEBUG (slice 21.4 prod investigation, remove once cause of prod pairCount=0 is found):
  // thread-confined per-request counters (each HTTP request runs on its own thread); reset at the
  // start of RecallSplitHalfReliability.compute and read back at the end.
  static final ThreadLocal<int[]> TEMP_DEBUG_COUNTERS = ThreadLocal.withInitial(() -> new int[3]);
  static final int TEMP_DEBUG_ACCURACY_NULL = 0;
  static final int TEMP_DEBUG_PACE_STATS_NULL = 1;
  static final int TEMP_DEBUG_DAY_BASELINE_NULL = 2;

  private static void tempDebugCount(int index) {
    TEMP_DEBUG_COUNTERS.get()[index]++;
  }

  enum Half {
    ODD,
    EVEN
  }

  /** Index values for both halves of one day. Either side may be {@code null}. */
  record HalfIndexes(Double odd, Double even) {}

  private RecallMorningHalfIndex() {}

  /**
   * Returns {@code null} when any of the four components can't be computed for this half (e.g. too
   * few qualifying rows/residuals in the half, or a day baseline that isn't warmed up yet) — 21.4
   * skips such half/mornings rather than guessing a partial composite.
   */
  static Double compute(
      List<RecallAnswerRow> allTimeReviews, LocalDate day, ZoneId zoneId, Half half) {
    return scoreHalf(prepareDay(allTimeReviews, day, zoneId), half);
  }

  /**
   * Scores both halves from one shared day-level setup. Either value may be {@code null} under the
   * same conditions as {@link #compute(List, LocalDate, ZoneId, Half)}.
   */
  static HalfIndexes computeBothHalves(
      List<RecallAnswerRow> allTimeReviews, LocalDate day, ZoneId zoneId) {
    DaySetup setup = prepareDay(allTimeReviews, day, zoneId);
    return new HalfIndexes(scoreHalf(setup, Half.ODD), scoreHalf(setup, Half.EVEN));
  }

  private record DaySetup(
      List<RecallAnswerRow> allTimeReviews,
      LocalDate day,
      ZoneId zoneId,
      List<RecallAnswerRow> dayQualifyingRowsInOrder,
      Map<QuestionType, ThreePlFit> accuracyFits) {}

  private static DaySetup prepareDay(
      List<RecallAnswerRow> allTimeReviews, LocalDate day, ZoneId zoneId) {
    PaceResult wholeDayPaceResult = RecallPaceAggregator.compute(allTimeReviews, day, zoneId);
    Set<RecallAnswerRow> implausiblyFastRows = wholeDayPaceResult.implausiblyFastRows();
    List<RecallAnswerRow> dayQualifyingRowsInOrder =
        dayQualifyingRowsInOrder(allTimeReviews, day, zoneId, implausiblyFastRows);
    List<RecallAnswerRow> allTimeQualifyingRows =
        allTimeReviews.stream().filter(r -> !implausiblyFastRows.contains(r)).toList();
    return new DaySetup(
        allTimeReviews,
        day,
        zoneId,
        dayQualifyingRowsInOrder,
        RecallAccuracyAggregator.fit(allTimeQualifyingRows, day, zoneId));
  }

  private static Double scoreHalf(DaySetup setup, Half half) {
    List<RecallAnswerRow> halfQualifyingRows = selectHalf(setup.dayQualifyingRowsInOrder(), half);
    Set<RecallAnswerRow> halfRows = Collections.newSetFromMap(new IdentityHashMap<>());
    halfRows.addAll(halfQualifyingRows);

    AccuracyStats accuracy =
        RecallAccuracyAggregator.apply(halfQualifyingRows, setup.accuracyFits());
    if (accuracy.getStandardizedResidual() == null) {
      tempDebugCount(TEMP_DEBUG_ACCURACY_NULL);
      return null;
    }
    // A is already an approximately-standardized residual (slices 17/19/20) where positive means
    // recalling *better* than expected — sign-flipped here so positive matches the composite's
    // "worse than usual" convention, per 21.2's deferred note.
    double zA = -accuracy.getStandardizedResidual();

    PaceResult halfPaceResult =
        RecallPaceAggregator.compute(setup.allTimeReviews(), setup.day(), setup.zoneId(), halfRows);
    PaceStats stats = halfPaceResult.stats();
    if (stats.getPctVsUsual() == null || stats.getConsistencyZScore() == null) {
      tempDebugCount(TEMP_DEBUG_PACE_STATS_NULL);
      return null;
    }
    Double zPace =
        RecallDayBaseline.zScoreAgainstDayBaseline(
            stats.getPctVsUsual(), halfPaceResult.paceDayBaseline());
    // Unlike zA, lapseCount's raw day-baseline z-score already has the right sign: a higher lapse
    // count than usual is a higher (positive) z, and a higher lapse count is already "worse" — the
    // sign-flip 21.2 deferred to this slice resolves to the identity here, not a negation.
    Double zLapse =
        RecallDayBaseline.zScoreAgainstDayBaseline(
            stats.getLapseCount(), halfPaceResult.lapseDayBaseline());
    if (zPace == null || zLapse == null) {
      tempDebugCount(TEMP_DEBUG_DAY_BASELINE_NULL);
      return null;
    }
    double zConsistency = stats.getConsistencyZScore();

    return RecallCognitiveIndex.compute(zA, zPace, zLapse, zConsistency);
  }

  /**
   * Day D's qualifying rows in chronological order — the same "answered today, counts as a review,
   * not an implausibly-fast mistap" definition {@link RecallStatsService#aggregateRows} already
   * uses to build {@code todaysQualifyingRows}, reused here rather than re-derived, since that is
   * exactly the ordering the odd/even split is defined against.
   */
  private static List<RecallAnswerRow> dayQualifyingRowsInOrder(
      List<RecallAnswerRow> allTimeReviews,
      LocalDate day,
      ZoneId zoneId,
      Set<RecallAnswerRow> implausiblyFastRows) {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (RecallAnswerRow r : allTimeReviews) {
      if (r.answerCreatedAt() == null || implausiblyFastRows.contains(r)) {
        continue;
      }
      LocalDate rowDate =
          TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId).toLocalDate();
      if (rowDate.equals(day)) {
        rows.add(r);
      }
    }
    return rows;
  }

  /**
   * 1-indexed by within-day chronological order: odd positions (1st, 3rd, ...) in one half, even
   * positions (2nd, 4th, ...) in the other. Returns the selected row objects themselves so callers
   * can identity-match them (matching {@code implausiblyFastRows}' existing precedent of tracking
   * specific row objects rather than relying on {@link RecallAnswerRow}'s structural equality).
   */
  private static List<RecallAnswerRow> selectHalf(
      List<RecallAnswerRow> dayQualifyingRowsInOrder, Half half) {
    List<RecallAnswerRow> selected = new ArrayList<>();
    for (int i = 0; i < dayQualifyingRowsInOrder.size(); i++) {
      boolean isOddPosition = (i + 1) % 2 == 1;
      if ((half == Half.ODD) == isOddPosition) {
        selected.add(dayQualifyingRowsInOrder.get(i));
      }
    }
    return selected;
  }
}
