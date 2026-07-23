package com.odde.doughnut.services;

import static com.odde.doughnut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.RecallStatsDTO;
import com.odde.doughnut.entities.RecallPrompt;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecallStatsServiceTest {

  @Nested
  class ResponseTimeTrimmedMean {
    @Test
    void usesThinkingTimeMsWhenNonNull() {
      Timestamp now = utc(11, 12); // today = 1989-01-11
      List<RecallPrompt> rows =
          List.of(
              answered(utc(9, 10), 5000, true, null),
              answered(utc(9, 11), 6000, true, null),
              answered(utc(9, 12), 7000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dayAvg(dto, "1989-01-10").getAvgMs(), equalTo(6000L));
    }

    @Test
    void dropsSub1000msMisclicks() {
      Timestamp now = utc(11, 12);
      List<RecallPrompt> rows =
          List.of(
              answered(utc(9, 10), 500, true, null), // dropped (<1000)
              answered(utc(9, 11), 5000, true, null),
              answered(utc(9, 12), 6000, true, null),
              answered(utc(9, 13), 7000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dayAvg(dto, "1989-01-10").getAvgMs(), equalTo(6000L));
      assertThat(dayAvg(dto, "1989-01-10").getSampleSize(), equalTo(3));
    }

    @Test
    void capsThinkingTimeMsAbove120000() {
      Timestamp now = utc(11, 12);
      List<RecallPrompt> rows =
          List.of(
              answered(utc(9, 10), 200000, true, null), // capped to 120000
              answered(utc(9, 11), 5000, true, null),
              answered(utc(9, 12), 5000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dayAvg(dto, "1989-01-10").getAvgMs(), equalTo(43333L));
    }

    @Test
    void fallsBackToDiffWhenThinkingTimeMsNull() {
      Timestamp now = utc(11, 12);
      // 3 answers on 1989-01-10, thinkingTimeMs null, diffs 10000/11000/12000 (uncapped)
      Timestamp p = utc(9, 8); // 1989-01-10 08:00 UTC
      List<RecallPrompt> rows =
          List.of(
              answered(new Timestamp(p.getTime() + 10_000), null, true, p),
              answered(new Timestamp(p.getTime() + 11_000), null, true, p),
              answered(new Timestamp(p.getTime() + 12_000), null, true, p));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dayAvg(dto, "1989-01-10").getAvgMs(), equalTo(11000L));
    }

    @Test
    void capsDiffFallbackAbove300000() {
      Timestamp now = utc(11, 12);
      Timestamp p = utc(9, 8);
      List<RecallPrompt> rows =
          List.of(
              answered(new Timestamp(p.getTime() + 400_000), null, true, p), // capped to 300000
              answered(new Timestamp(p.getTime() + 400_000), null, true, p),
              answered(new Timestamp(p.getTime() + 400_000), null, true, p));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dayAvg(dto, "1989-01-10").getAvgMs(), equalTo(300000L));
    }

    @Test
    void returnsNullAvgWhenFewerThan3ValidSamples() {
      Timestamp now = utc(11, 12);
      List<RecallPrompt> rows =
          List.of(answered(utc(9, 10), 5000, true, null), answered(utc(9, 11), 6000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dayAvg(dto, "1989-01-10").getAvgMs(), nullValue());
      assertThat(dayAvg(dto, "1989-01-10").getSampleSize(), equalTo(2));
    }
  }

  @Nested
  class RetentionAggregation {
    @Test
    void perDayRetentionIsCorrectOverAnsweredWithGuard() {
      Timestamp now = utc(11, 12);
      // 1989-01-10: 2 correct / 2 answered -> insufficient (<3) -> null
      // 1989-01-09: 3 correct / 4 answered -> 75%
      List<RecallPrompt> rows =
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
      assertThat(dayRet(dto, "1989-01-10").getAnsweredCount(), equalTo(2));
      assertThat(dayRet(dto, "1989-01-09").getRetentionPct(), closeTo(75.0, 0.01));
      assertThat(dayRet(dto, "1989-01-09").getCorrectCount(), equalTo(3));
      assertThat(dayRet(dto, "1989-01-09").getAnsweredCount(), equalTo(4));
    }

    @Test
    void overallRetentionPct365OverTheWindow() {
      Timestamp now = utc(11, 12);
      List<RecallPrompt> rows =
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
      List<RecallPrompt> rows =
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
      List<RecallPrompt> rows =
          List.of(
              answered(utc(8, 10), 5000, true, null),
              answered(utc(8, 10), 5000, false, null),
              answered(utc(8, 10), 5000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dto.getWeekdayHourCounts()[0][10], equalTo(3));
      assertThat(dto.getWeekdayHourCorrect()[0][10], equalTo(2));
    }
  }

  @Nested
  class TimezoneBucketing {
    @Test
    void shanghai10amAndUtc10amLandInDifferentLocalDayBuckets() {
      // Same UTC instant 1989-01-09 17:00 UTC:
      //   under UTC zone -> local day 1989-01-09
      //   under Shanghai zone -> 1989-01-10 01:00 -> local day 1989-01-10
      Timestamp instant = utc(8, 17);
      Timestamp now = utc(20, 12);
      List<RecallPrompt> rows =
          List.of(answered(instant, 5000, true, null), answered(instant, 5000, true, null));
      RecallStatsDTO asShanghai = aggregateZone(rows, ZoneId.of("Asia/Shanghai"), now);
      RecallStatsDTO asUtc = aggregateZone(rows, ZoneId.of("UTC"), now);
      assertThat(calendarCount(asShanghai, "1989-01-10"), equalTo(2L));
      assertThat(calendarCount(asUtc, "1989-01-09"), equalTo(2L));
    }
  }

  @Nested
  class Streak {
    @Test
    void currentStreakEndsAtTodayAndLongestIsMaxRun() {
      // now = 1989-01-11 12:00 UTC. today = 1989-01-11.
      // reviews on day 10, 9, 8 (consecutive ending today) -> current 3.
      // also day 5, 4 (run of 2). longest = 3.
      Timestamp now = utc(11, 12);
      List<RecallPrompt> rows =
          List.of(
              answered(utc(11, 8), 5000, true, null),
              answered(utc(10, 8), 5000, true, null),
              answered(utc(9, 8), 5000, true, null),
              answered(utc(6, 8), 5000, true, null),
              answered(utc(5, 8), 5000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dto.getTotals().getCurrentStreak(), equalTo(3));
      assertThat(dto.getTotals().getLongestStreak(), equalTo(3));
    }

    @Test
    void currentStreakZeroWhenNoReviewToday() {
      Timestamp now = utc(11, 12);
      List<RecallPrompt> rows =
          List.of(answered(utc(9, 8), 5000, true, null), answered(utc(8, 8), 5000, true, null));
      RecallStatsDTO dto = aggregate(rows, now);
      assertThat(dto.getTotals().getCurrentStreak(), equalTo(0));
      assertThat(dto.getTotals().getLongestStreak(), equalTo(2));
    }
  }

  @Nested
  class CalendarShape {
    @Test
    void calendarHas365ZeroFilledEntries() {
      Timestamp now = utc(11, 12);
      RecallStatsDTO dto = aggregate(List.of(), now);
      assertThat(dto.getCalendar().size(), equalTo(365));
      assertThat(dto.getCalendar(), everyItem(hasProperty("count", equalTo(0))));
    }

    @Test
    void trendAndRetentionTrendHave90Entries() {
      Timestamp now = utc(11, 12);
      RecallStatsDTO dto = aggregate(List.of(), now);
      assertThat(dto.getTrend().size(), equalTo(90));
      assertThat(dto.getRetentionTrend().size(), equalTo(90));
    }
  }
}
