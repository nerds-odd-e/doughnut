package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.answered;
import static com.odde.donut.services.RecallStatsTestFixtures.utc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.services.RecallPaceAggregator.PaceResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link RecallPaceAggregator}'s per-day cross-morning baselines for {@code pctVsUsual} and
 * {@code lapseCount} (slice 21.1) — the same shape of baseline {@code consistencyZScore} already
 * has (see {@link RecallStatsServiceConsistencyAggregationTest}). Not yet turned into a z-score or
 * wired into {@link com.odde.donut.controllers.dto.RecallStatsDTO}, so this exercises the
 * package-private aggregator directly rather than through the DTO.
 *
 * <p>Baseline window is {@code [today - 63, today - 4]} (60 days). "Today" here is day 100 (see
 * {@link RecallStatsTestFixtures#utc}), so the window spans days 37..96.
 */
class RecallPaceAggregatorDayBaselineTest {
  private static final int TODAY = 100;
  private static final int BASELINE_START = TODAY - 63; // 37
  private static final int BASELINE_END = TODAY - 4; // 96
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final LocalDate TODAY_DATE = LocalDate.of(1989, 1, 1).plusDays(TODAY);
  private static final int BASELINE_MS = 5000;

  /**
   * Establishes a fresh item's EWMA baseline at {@link #BASELINE_MS} with an answer on day 0 (well
   * before {@link #BASELINE_START}), then answers it once on {@code day} at {@code onTaskMs} so
   * that single answer becomes {@code day}'s only qualifying residual/lapse-count contribution.
   */
  private static void addDay(List<RecallAnswerRow> rows, int day, int itemId, int onTaskMs) {
    rows.add(answered(utc(0, 6), BASELINE_MS, true, null, itemId));
    rows.add(answered(utc(day, 10), onTaskMs, true, null, itemId));
  }

  private static PaceResult compute(List<RecallAnswerRow> rows) {
    return RecallPaceAggregator.compute(rows, TODAY_DATE, UTC);
  }

  @Test
  void fewerThanTenQualifyingDaysYieldsNullBaselinesForBothPaceAndLapse() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      addDay(rows, BASELINE_START + i, 1000 + i, BASELINE_MS);
    }
    PaceResult result = compute(rows);
    assertThat(result.paceDayBaseline().median(), nullValue());
    assertThat(result.paceDayBaseline().mad(), nullValue());
    assertThat(result.lapseDayBaseline().median(), nullValue());
    assertThat(result.lapseDayBaseline().mad(), nullValue());
  }

  @Test
  void paceDayBaselineReusesTodaysWeightedMedianTransformPerDay() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    int itemId = 2000;
    for (int day = BASELINE_START; day <= BASELINE_END; day++) {
      // 30 days at -10% of baseline, 30 days at +20% of baseline.
      int onTaskMs = ((day - BASELINE_START) % 2 == 0) ? 4500 : 6000;
      addDay(rows, day, itemId++, onTaskMs);
    }
    PaceResult result = compute(rows);
    // Each day has exactly one residual, so that day's weighted-median transform (the same one
    // used for today's pctVsUsual) just reduces to that residual's own pctVsUsual: exactly -10.0
    // and +20.0. Median of the two is 5.0; MAD is 15.0 (every value is exactly 15 from 5.0).
    assertThat(result.paceDayBaseline().median(), closeTo(5.0, 0.01));
    assertThat(result.paceDayBaseline().mad(), closeTo(15.0, 0.01));
  }

  @Test
  void lapseDayBaselineRecordsPlainPerDayCountAcrossQualifyingDays() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    int itemId = 3000;
    for (int day = BASELINE_START; day <= BASELINE_END; day++) {
      // 30 days with one lapse (correct, >= 2.5x the 5000ms baseline), 30 days with none.
      int onTaskMs = ((day - BASELINE_START) % 2 == 0) ? 15000 : BASELINE_MS;
      addDay(rows, day, itemId++, onTaskMs);
    }
    PaceResult result = compute(rows);
    // Per-day lapse counts are exactly 1 and 0 on alternating days; median is 0.5, MAD is 0.5.
    assertThat(result.lapseDayBaseline().median(), closeTo(0.5, 0.001));
    assertThat(result.lapseDayBaseline().mad(), closeTo(0.5, 0.001));
  }
}
