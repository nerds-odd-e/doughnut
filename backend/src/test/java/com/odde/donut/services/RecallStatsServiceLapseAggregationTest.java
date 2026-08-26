package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Split out of {@link RecallStatsServiceTest} to keep each test class under the 250-line limit. */
class RecallStatsServiceLapseAggregationTest {
  @Test
  void correctAnswerAtLeast2point5xBaselineTodayIsCountedAsRetrievalLapse() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~5000ms baseline for item 15
            answered(utc(9, 10), 5000, true, null, 15),
            answered(utc(9, 11), 5000, true, null, 15),
            // today: 13000ms is >= 2.5x the 5000ms baseline, and correct
            answered(utc(11, 10), 13000, true, null, 15));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getLapseCount(), equalTo(1));
  }

  @Test
  void slowIncorrectAnswerIsNotCountedAsRetrievalLapse() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(9, 10), 5000, true, null, 16),
            answered(utc(9, 11), 5000, true, null, 16),
            // today: 13000ms is >= 2.5x baseline, but wrong -> a knowledge gap, not a lapse
            answered(utc(11, 10), 13000, false, null, 16));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getLapseCount(), equalTo(0));
  }

  @Test
  void correctAnswerJustUnder2point5xBaselineIsNotCountedAsRetrievalLapse() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(9, 10), 5000, true, null, 17),
            answered(utc(9, 11), 5000, true, null, 17),
            // today: 10000ms is only 2x the 5000ms baseline -> under the 2.5x lapse threshold
            answered(utc(11, 10), 10000, true, null, 17));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getLapseCount(), equalTo(0));
  }

  @Test
  void hardDroppedSlowCorrectAnswerIsNotCountedAsRetrievalLapse() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    Timestamp p = utc(11, 9);
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 18
            answered(utc(9, 10), 20000, true, null, 18),
            answered(utc(9, 11), 20000, true, null, 18),
            // hard-dropped: diff-fallback caps to 300000ms (>= 5 min threshold); this would be
            // >= 2.5x the 20000ms baseline (50000ms) but is excluded like any hard-dropped row
            answered(new Timestamp(p.getTime() + 400_000), null, true, p, 18));
    RecallStatsDTO dto = aggregate(rows, now);
    assertThat(dto.getPace().getLapseCount(), equalTo(0));
  }
}
