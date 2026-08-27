package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pace-tile tests for excluding/winsorizing implausible on-task times (mistaps, outliers,
 * hard-drops) — split out of {@link RecallStatsServicePaceAggregationTest} to keep each test class
 * under the 250-line limit.
 */
class RecallStatsServicePaceExclusionTest {
  @Test
  void implausiblyFastMistapIsExcludedAndDoesNotPolluteItemBaseline() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 4 -> item floor = max(300, 0.25*20000) =
            // 5000ms
            answered(utc(9, 10), 20000, true, null, 4),
            answered(utc(9, 11), 20000, true, null, 4),
            // implausibly fast mistap today: 1500ms clears the pre-existing absolute 1000ms
            // floor (so this exercises the NEW item-relative floor, not the old one) but is
            // still well under the 5000ms item-relative floor
            answered(utc(11, 9), 1500, true, null, 4),
            // a normal-speed answer today, later, should compare against the pre-mistap
            // baseline (still ~20000ms), not a baseline corrupted by the 1500ms mistap
            answered(utc(11, 10), 20000, true, null, 4));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    // only the final normal-speed answer counts as a residual; the mistap is excluded
    assertThat(pace.getSampleSize(), equalTo(1));
    // the normal-speed answer matches the established (uncorrupted) baseline -> ~0% vs usual
    assertThat(pace.getPctVsUsual(), closeTo(0.0, 5.0));
  }

  @Test
  void singleVerySlowAttemptIsWinsorizedInsteadOfSwampingPace() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // baseline ~5000ms for item 5
            answered(utc(9, 10), 5000, true, null, 5),
            answered(utc(9, 11), 5000, true, null, 5),
            // today: 20x baseline -> raw residual ln(20) ~= 2.996, well above the ln(8) cap
            answered(utc(11, 10), 100000, true, null, 5));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(1));
    // capped residual -> (exp(ln 8) - 1) * 100 = 700%, not the raw ~exp(2.996)-1 ~= 1900%
    assertThat(pace.getPctVsUsual(), closeTo(700.0, 5.0));
  }

  @Test
  void attemptOver5MinutesIsHardDroppedAndDoesNotPolluteBaseline() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    Timestamp p = utc(11, 9);
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 6
            answered(utc(9, 10), 20000, true, null, 6),
            answered(utc(9, 11), 20000, true, null, 6),
            // hard-dropped: elapsed 400000ms diff-fallback caps to 300000ms (>= 5 min
            // threshold)
            answered(new Timestamp(p.getTime() + 400_000), null, true, p, 6),
            // a normal-speed answer today, later, should compare against the pre-drop
            // baseline (still ~20000ms), not one corrupted by the hard-dropped attempt
            answered(utc(11, 10), 20000, true, null, 6));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    // only the final normal-speed answer counts as a residual; the hard-dropped row is excluded
    assertThat(pace.getSampleSize(), equalTo(1));
    // the normal-speed answer matches the established (uncorrupted) baseline -> ~0% vs usual
    assertThat(pace.getPctVsUsual(), closeTo(0.0, 5.0));
    // the hard-dropped row still counts toward totalAnsweredToday (retention is unaffected)
    assertThat(pace.getTotalAnsweredToday(), equalTo(2));
  }

  @Test
  void thinkingTimeAbove120sIsNotClampedToTheTrendChartCapForPctVsUsual() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // baseline ~120000ms (the trend-chart's 120s cap) for item 20
            answered(utc(9, 10), 120000, true, null, 20),
            answered(utc(9, 11), 120000, true, null, 20),
            // today: 180000ms (3 min). If this were wrongly clamped to 120000ms (2 min), it
            // would exactly match the baseline and yield ~0% vs usual instead.
            answered(utc(11, 10), 180000, true, null, 20));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    assertThat(pace.getSampleSize(), equalTo(1));
    // ln(180000/120000) = ln(1.5) -> (1.5 - 1) * 100 = 50%
    assertThat(pace.getPctVsUsual(), closeTo(50.0, 5.0));
  }

  @Test
  void thinkingTimeMsAtOrAbove5MinutesIsHardDroppedWithoutGoingThroughDiffFallback() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 21
            answered(utc(9, 10), 20000, true, null, 21),
            answered(utc(9, 11), 20000, true, null, 21),
            // hard-dropped via the real thinkingTimeMs field itself, not the diff-fallback
            // path: this used to be clamped to 120000ms (well under the 300000ms hard-drop
            // threshold) and so wrongly counted as a residual.
            answered(utc(11, 9), 300000, true, null, 21),
            // a normal-speed answer today, later, should compare against the pre-drop
            // baseline (still ~20000ms), not one corrupted by the hard-dropped attempt
            answered(utc(11, 10), 20000, true, null, 21));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.PaceStats pace = dto.getPace();
    // only the final normal-speed answer counts as a residual; the hard-dropped row is excluded
    assertThat(pace.getSampleSize(), equalTo(1));
    assertThat(pace.getPctVsUsual(), closeTo(0.0, 5.0));
    assertThat(pace.getTotalAnsweredToday(), equalTo(2));
  }
}
