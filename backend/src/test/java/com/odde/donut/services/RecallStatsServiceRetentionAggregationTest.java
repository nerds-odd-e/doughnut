package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Split out of {@link RecallStatsServiceTest} to keep each test class under the 250-line limit. */
class RecallStatsServiceRetentionAggregationTest {
  @Test
  void perDayRetentionIsCorrectOverAnsweredWithGuard() {
    Timestamp now = utc(11, 12);
    // 1989-01-10: 2 correct / 2 answered -> insufficient (<3) -> null
    // 1989-01-09: 3 correct / 4 answered -> 75%
    List<RecallAnswerRow> rows =
        new ArrayList<>(
            List.of(
                answered(utc(9, 10), 5000, true, null),
                answered(utc(9, 11), 5000, true, null),
                answered(utc(8, 10), 5000, true, null),
                answered(utc(8, 11), 5000, true, null),
                answered(utc(8, 12), 5000, true, null),
                answered(utc(8, 13), 5000, false, null)));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dayRet(dto, "1989-01-10").getRetentionPct(), nullValue());
    assertThat(dayRet(dto, "1989-01-09").getRetentionPct(), closeTo(75.0, 0.01));
  }

  @Test
  void overallRetentionPct365OverTheWindow() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(9, 10), 5000, true, null),
            answered(utc(9, 11), 5000, true, null),
            answered(utc(9, 12), 5000, true, null),
            answered(utc(9, 13), 5000, false, null));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getTotals().getRetentionPct365(), closeTo(75.0, 0.01));
  }

  @Test
  void bestAndWorstHourByRetentionWithMin5Guard() {
    Timestamp now = utc(11, 12);
    // hour 10: 5/5 correct -> 100% (best)
    // hour 11: 1/5 correct -> 20% (worst)
    List<RecallAnswerRow> rows =
        new ArrayList<>(
            List.of(
                answered(utc(9, 10), 5000, true, null),
                answered(utc(8, 10), 5000, true, null),
                answered(utc(7, 10), 5000, true, null),
                answered(utc(6, 10), 5000, true, null),
                answered(utc(5, 10), 5000, true, null),
                answered(utc(9, 11), 5000, true, null),
                answered(utc(8, 11), 5000, false, null),
                answered(utc(7, 11), 5000, false, null),
                answered(utc(6, 11), 5000, false, null),
                answered(utc(5, 11), 5000, false, null)));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getTotals().getBestHour(), equalTo(10));
    assertThat(dto.getTotals().getBestHourRetentionPct(), closeTo(100.0, 0.01));
    assertThat(dto.getTotals().getWorstHour(), equalTo(11));
    assertThat(dto.getTotals().getWorstHourRetentionPct(), closeTo(20.0, 0.01));
  }

  @Test
  void weekdayHourCorrectAndCountsFromSameRows() {
    Timestamp now = utc(11, 12);
    // 1989-01-09 is a Monday (DayOfWeek=1 -> idx 0), hour 10
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(8, 10), 5000, true, null),
            answered(utc(8, 10), 5000, false, null),
            answered(utc(8, 10), 5000, true, null));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getWeekdayHourCounts()[0][10], equalTo(3));
    assertThat(dto.getWeekdayHourCorrect()[0][10], equalTo(2));
  }

  @Test
  void implausiblyFastCorrectAnswerIsExcludedFromRetentionButNormalSpeedCounts() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 6 -> item floor = max(300, 0.25*20000) =
            // 5000ms
            answered(utc(9, 10), 20000, true, null, 6),
            answered(utc(9, 11), 20000, true, null, 6),
            // implausibly fast mistap: clears the pre-existing absolute 1000ms floor but is
            // well under the 5000ms item-relative floor -> must not count toward retention
            answered(utc(9, 12), 1500, true, null, 6),
            // normal-speed correct answer for the same item -> must count toward retention
            answered(utc(9, 13), 20000, true, null, 6));
    RecallStatsDTO dto = aggregate(rows, now);
    // 4 rows total, but the mistap is dropped entirely: 3 answered, all correct -> 100%
    assertThat(dto.getTotals().getTotalReviews365(), equalTo(3));
    assertThat(dto.getTotals().getRetentionPct365(), closeTo(100.0, 0.01));
  }

  @Test
  void implausiblyFastMistapBelowOldOneSecondDropIsAlsoExcludedFromRetention() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 19 -> item floor = max(300, 0.25*20000) =
            // 5000ms
            answered(utc(9, 10), 20000, true, null, 19),
            answered(utc(9, 11), 20000, true, null, 19),
            // 200ms mistap: below the trend-chart aggregator's 1000ms drop threshold (which
            // used to make this row silently skip the implausibly-fast check entirely and
            // still count toward retention) but still well under the item-relative 5000ms
            // floor -> must be excluded from retention just like the 1500ms case above.
            answered(utc(9, 12), 200, true, null, 19),
            answered(utc(9, 13), 20000, true, null, 19));
    RecallStatsDTO dto = aggregate(rows, now);
    // 4 rows total, but the 200ms mistap is dropped entirely: 3 answered, all correct -> 100%
    assertThat(dto.getTotals().getTotalReviews365(), equalTo(3));
    assertThat(dto.getTotals().getRetentionPct365(), closeTo(100.0, 0.01));
  }

  @Test
  void itemWithNoPriorBaselineCountsTowardRetentionAboveFlat300msFloor() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            // item 7's very first-ever answer: no baseline yet, so the item-relative floor
            // falls back to the flat 300ms floor. 1000ms clears both that and the pre-existing
            // absolute floor, so it must count normally toward retention.
            answered(utc(9, 10), 1000, true, null, 7));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getTotals().getTotalReviews365(), equalTo(1));
    assertThat(dto.getTotals().getRetentionPct365(), closeTo(100.0, 0.01));
  }
}
