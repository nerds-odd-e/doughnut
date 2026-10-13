package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallStatsExcludedAttemptIsNotAReviewTest {
  Timestamp now = utc(11, 12);
  RecallAnswerRow ordinary = answered(utc(11, 9), 5000, true, null, 1, 0.9);
  RecallAnswerRow excluded = excludedAnswered(utc(11, 10), utc(11, 11));

  @Test
  void excludedAttemptContributesToNoneOfReviewsRetentionAccuracyOrCalibration() {
    RecallStatsDTO dto = aggregate(List.of(ordinary, excluded), now);

    assertThat(dto, equalTo(aggregate(List.of(ordinary), now)));
    assertThat(dto.getTotals().getTotalReviewsAllTime(), equalTo(1));
    assertThat(dto.getTotals().getRetentionPct365(), equalTo(100.0));
    assertThat(calendarCount(dto, "1989-01-12"), equalTo(1L));
    assertThat(dto.getAccuracy().getSampleSize(), equalTo(1));
  }

  @Test
  void anExcludedAttemptAloneLeavesNoReview() {
    RecallStatsDTO dto = aggregate(List.of(excluded), now);

    assertThat(dto.getTotals().getTotalReviewsAllTime(), equalTo(0));
    assertThat(dto.getTotals().getRetentionPct365(), nullValue());
    assertThat(dto.getAccuracy().getSampleSize(), equalTo(0));
  }
}
