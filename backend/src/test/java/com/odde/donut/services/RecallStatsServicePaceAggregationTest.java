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
 * Tests for excluding/winsorizing implausible on-task times live in {@link
 * RecallStatsServicePaceExclusionTest} instead.
 */
class RecallStatsServicePaceAggregationTest {
  @Test
  void itemSlowerThanUsualTodayYieldsPositivePctVsUsual() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(9, 10), 5000, true, null, 1),
            answered(utc(9, 11), 5000, true, null, 1),
            answered(utc(11, 10), 20000, true, null, 1));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(1));
    assertThat(pace.getPctVsUsual(), closeTo(300.0, 5.0));
  }

  @Test
  void itemFasterThanUsualTodayYieldsNegativePctVsUsual() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(9, 10), 20000, true, null, 2),
            answered(utc(9, 11), 20000, true, null, 2),
            answered(utc(11, 10), 5000, true, null, 2));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getPctVsUsual(), lessThan(0.0));
  }

  @Test
  void itemWithNoPriorHistoryTodayExcludedFromSampleSizeButCountsTowardTotalAnsweredToday() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows = List.of(answered(utc(11, 10), 5000, true, null, 3));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(0));
    assertThat(pace.getPctVsUsual(), nullValue());
    assertThat(pace.getTotalAnsweredToday(), equalTo(1));
  }

  @Test
  void sampleSizeZeroWhenNoQualifyingResiduals() {
    Timestamp now = utc(11, 12);
    RecallStatsDTO dto = aggregate(List.of(), now);
    assertThat(dto.getPace().getSampleSize(), equalTo(0));
    assertThat(dto.getPace().getPctVsUsual(), nullValue());
  }

  @Test
  void medianResistsBeingPulledByASingleCappedHighOutlier() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // item 7: baseline ~5000ms, today's answer matches it -> residual ~0
            answered(utc(9, 10), 5000, true, null, 7),
            answered(utc(9, 11), 5000, true, null, 7),
            answered(utc(11, 10), 5000, true, null, 7),
            // item 8: baseline ~5000ms, today's answer matches it -> residual ~0
            answered(utc(9, 10), 5000, true, null, 8),
            answered(utc(9, 11), 5000, true, null, 8),
            answered(utc(11, 10), 5000, true, null, 8),
            // item 9: baseline ~5000ms, today's answer is a 20x outlier -> capped at ln(8)
            answered(utc(9, 10), 5000, true, null, 9),
            answered(utc(9, 11), 5000, true, null, 9),
            answered(utc(11, 10), 100000, true, null, 9));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(3));
    // median of {~0, ~0, capped-high} stays near 0%; a mean would be pulled toward ~230%+
    assertThat(pace.getPctVsUsual(), closeTo(0.0, 5.0));
  }

  @Test
  void establishedItemDominatesWeightedMedianOverColdStartItemsResidual() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows = new ArrayList<>();
    // item 10: 27 prior valid answers -> m_j = 27 -> w = 27/30 = 0.9, near-full confidence.
    // Today's answer matches the established ~5000ms baseline -> residual ~0.
    for (int i = 0; i < 27; i++) {
      rows.add(answered(utc(9, 10), 5000, true, null, 10));
    }
    rows.add(answered(utc(11, 10), 5000, true, null, 10));
    // item 11: a single prior answer -> m_j = 1 -> w = 1/4 = 0.25 (cold-start, low confidence).
    // Today's answer is a 20x outlier -> capped residual, huge unweighted pct.
    rows.add(answered(utc(9, 10), 5000, true, null, 11));
    rows.add(answered(utc(11, 10), 100000, true, null, 11));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(2));
    // the established item's near-full weight (0.9) outweighs the cold-start item's (0.25),
    // so the weighted median lands on the established item's ~0% residual, not a value pulled
    // toward the cold-start item's capped-outlier ~700%.
    assertThat(pace.getPctVsUsual(), closeTo(0.0, 5.0));
    assertThat(pace.getConfidence(), closeTo(0.575, 0.01)); // mean(0.9, 0.25)
  }

  @Test
  void allColdStartMorningYieldsLowConfidenceButStillComputesPctVsUsual() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // item 12: single prior answer -> m_j = 1 -> w = 0.25 (the minimum weight any
            // residual-producing row can carry, since a residual requires a prior baseline).
            answered(utc(9, 10), 5000, true, null, 12),
            answered(utc(11, 10), 6000, true, null, 12),
            // item 13: same cold-start shape, faster than its single prior baseline.
            answered(utc(9, 10), 5000, true, null, 13),
            answered(utc(11, 10), 4000, true, null, 13));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(2));
    // every contributing item is cold-start (m_j = 1, the practical floor) -> low confidence,
    // but total weight is still positive so pctVsUsual is computed, not null.
    assertThat(pace.getConfidence(), closeTo(0.25, 0.001));
    assertThat(pace.getPctVsUsual(), notNullValue());
    // equal weights -> weighted median picks the lower of the two residuals (item 13's -20%).
    assertThat(pace.getPctVsUsual(), closeTo(-20.0, 1.0));
  }

  @Test
  void establishedItemWithLargeMYieldsConfidenceNearOne() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows = new ArrayList<>();
    // item 14: 97 prior valid answers -> m_j = 97 -> w = 97/100 = 0.97, near-full confidence.
    for (int i = 0; i < 97; i++) {
      rows.add(answered(utc(9, 10), 5000, true, null, 14));
    }
    rows.add(answered(utc(11, 10), 5000, true, null, 14));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(1));
    assertThat(pace.getConfidence(), closeTo(0.97, 0.001));
    assertThat(pace.getPctVsUsual(), closeTo(0.0, 5.0));
  }
}
