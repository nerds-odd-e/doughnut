package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Split out of {@link RecallStatsServiceTest} to keep each test class under the 250-line limit.
 *
 * <p>Baseline window is {@code [today - 63, today - 4]} (60 days, excluding today and the 3 days
 * immediately before it). "Today" in these tests is day 100 (see {@link
 * RecallStatsTestFixtures#utc}), so the baseline window spans days 37..96 and days 97, 98, 99 are
 * excluded.
 */
class RecallStatsServiceConsistencyAggregationTest {
  private static final int TODAY = 100;
  private static final int BASELINE_START = TODAY - 63; // 37
  private static final int BASELINE_END = TODAY - 4; // 96

  /**
   * Adds a two-item pair on {@code day} that produces exactly one residual of {@code -spread} and
   * one of {@code +spread}, so the day's MAD (spread) equals {@code spread}. Each item is
   * established with a 5000ms answer earlier the same day, then measured against that baseline.
   */
  private static void addDayWithSpread(
      List<RecallAnswerRow> rows, int day, double spread, int itemIdBase) {
    int establishRt = 5000;
    int lowRt = (int) Math.round(establishRt * Math.exp(-spread));
    int highRt = (int) Math.round(establishRt * Math.exp(spread));
    rows.add(answered(utc(day, 6), establishRt, true, null, itemIdBase));
    rows.add(answered(utc(day, 7), lowRt, true, null, itemIdBase));
    rows.add(answered(utc(day, 8), establishRt, true, null, itemIdBase + 1));
    rows.add(answered(utc(day, 9), highRt, true, null, itemIdBase + 1));
  }

  /**
   * Gives {@code itemId} {@code priorObservationCount} valid answers before today, all at {@code
   * baselineMs} so its EWMA baseline stays exactly {@code ln(baselineMs)}, spread across days
   * 0..priorObservationCount-1 (well before {@link #BASELINE_START}) so they don't contribute to
   * {@code residualsByDate}.
   */
  private static void establishItem(
      List<RecallAnswerRow> rows, int itemId, int priorObservationCount, int baselineMs) {
    for (int day = 0; day < priorObservationCount; day++) {
      rows.add(answered(utc(day, 6), baselineMs, true, null, itemId));
    }
  }

  @Test
  void coldStartResidualsDoNotDominateConsistencyBadgeWhenOneItemIsWellEstablished() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    // 60 qualifying baseline days, spread alternating 0.009/0.011 -> median 0.01, MAD 0.001.
    for (int day = BASELINE_START; day <= BASELINE_END; day++) {
      double spread = (day % 2 == 0) ? 0.009 : 0.011;
      addDayWithSpread(rows, day, spread, 1000 + (day - BASELINE_START) * 10);
    }
    // One well-established item (30 prior observations -> weight 30/33 ~= 0.91): today's answer
    // matches its baseline exactly, so its own residual is tight/consistent.
    int establishedItemId = 8000;
    establishItem(rows, establishedItemId, 30, 5000);
    rows.add(answered(utc(TODAY, 10), 5000, true, null, establishedItemId));
    // Several cold-start items (1 prior observation each -> weight 1/4 = 0.25): today's answers
    // are wildly spread relative to their own barely-formed baseline.
    int coldStartFastA = 9001;
    int coldStartFastB = 9002;
    int coldStartSlow = 9003;
    establishItem(rows, coldStartFastA, 1, 5000);
    establishItem(rows, coldStartFastB, 1, 5000);
    establishItem(rows, coldStartSlow, 1, 5000);
    rows.add(answered(utc(TODAY, 11), 1300, true, null, coldStartFastA));
    rows.add(answered(utc(TODAY, 12), 1300, true, null, coldStartFastB));
    rows.add(answered(utc(TODAY, 13), 39000, true, null, coldStartSlow));

    Timestamp now = utc(TODAY, 14);
    RecallStatsDTO dto = aggregate(rows, now);
    // The established item alone (weight ~0.91) outweighs all three cold-start items combined
    // (weight 0.75), so the weighted spread should stay near the established item's tight
    // residual (~0) rather than being dragged up by cold-start noise.
    assertThat(dto.getPace().getConsistencyZScore(), notNullValue());
    assertThat(dto.getPace().getConsistencyZScore(), lessThan(1.0));
  }

  @Test
  void fewerThanTenQualifyingBaselineDaysYieldsNullConsistencyZScoreEvenWithPlentyOfToday() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    // Only 5 qualifying baseline days -> below the 10-day minimum.
    for (int i = 0; i < 5; i++) {
      addDayWithSpread(rows, BASELINE_START + i, 0.01, 1000 + i * 10);
    }
    // Plenty of today's residuals, tight spread.
    addDayWithSpread(rows, TODAY, 0.01, 9000);
    addDayWithSpread(rows, TODAY, 0.01, 9010);
    Timestamp now = utc(TODAY, 12);
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getConsistencyZScore(), nullValue());
  }

  @Test
  void establishedTightBaselineWithSimilarlyTightTodayYieldsZScoreNearZero() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    // 60 qualifying baseline days, spread alternating 0.009/0.011 -> median 0.01, MAD 0.001.
    for (int day = BASELINE_START; day <= BASELINE_END; day++) {
      double spread = (day % 2 == 0) ? 0.009 : 0.011;
      addDayWithSpread(rows, day, spread, 1000 + (day - BASELINE_START) * 10);
    }
    // Today's spread matches the baseline median (0.01) -> z-score should be near 0.
    addDayWithSpread(rows, TODAY, 0.01, 9000);
    // A third, equal-weight item with a residual of 0 breaks the exact half-weight tie between
    // the two symmetric points above (today's spread is now weighted, and weightedMedian resolves
    // an exact tie by picking the lower value rather than averaging), so the weighted spread still
    // reflects the intended 0.01.
    establishItem(rows, 9500, 1, 5000);
    rows.add(answered(utc(TODAY, 20), 5000, true, null, 9500));
    Timestamp now = utc(TODAY, 21);
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getConsistencyZScore(), closeTo(0.0, 0.3));
  }

  @Test
  void todayFarMoreSpreadOutThanBaselineYieldsClearlyPositiveZScore() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int day = BASELINE_START; day <= BASELINE_END; day++) {
      double spread = (day % 2 == 0) ? 0.009 : 0.011;
      addDayWithSpread(rows, day, spread, 1000 + (day - BASELINE_START) * 10);
    }
    // Today's spread is far larger than any baseline day.
    addDayWithSpread(rows, TODAY, 0.5, 9000);
    // A third, equal-weight item with a residual of 0 breaks the exact half-weight tie between
    // the two symmetric points above (today's spread is now weighted, and weightedMedian resolves
    // an exact tie by picking the lower value rather than averaging), so the weighted spread still
    // reflects the intended 0.5.
    establishItem(rows, 9500, 1, 5000);
    rows.add(answered(utc(TODAY, 20), 5000, true, null, 9500));
    Timestamp now = utc(TODAY, 21);
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getConsistencyZScore(), notNullValue());
    assertThat(dto.getPace().getConsistencyZScore(), greaterThan(1.0));
  }

  @Test
  void dayWithinExcludedLastThreeDaysDoesNotContributeToBaseline() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    // Only 9 qualifying days inside the real baseline window.
    for (int i = 0; i < 9; i++) {
      addDayWithSpread(rows, BASELINE_START + i, 0.01, 1000 + i * 10);
    }
    // A 10th day that WOULD qualify but falls within the excluded last-3-days window
    // (today - 3 == day 97).
    addDayWithSpread(rows, TODAY - 3, 0.01, 2000);
    addDayWithSpread(rows, TODAY, 0.01, 9000);
    Timestamp now = utc(TODAY, 12);
    RecallStatsDTO dto = aggregate(rows, now);
    // If the excluded day were wrongly counted, qualifying days would reach 10 and this would be
    // non-null; correctly excluding it keeps the count at 9 -> null.
    assertThat(dto.getPace().getConsistencyZScore(), nullValue());
  }
}
