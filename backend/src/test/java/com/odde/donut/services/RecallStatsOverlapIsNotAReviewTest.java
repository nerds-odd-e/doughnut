package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallStatsOverlapIsNotAReviewTest {
  @Test
  void overlapAnswerContributesToNoneOfReviewsRetentionOrCalendar() {
    Timestamp now = utc(11, 12);
    RecallStatsDTO dto = aggregate(List.of(overlapAnswered(utc(9, 10))), now);
    assertThat(dto.getTotals().getTotalReviewsAllTime(), equalTo(0));
    assertThat(dto.getTotals().getRetentionPct365(), nullValue());
    assertThat(calendarCount(dto, "1989-01-10"), equalTo(0L));
  }
}
