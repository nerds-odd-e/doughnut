package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import com.odde.doughnut.controllers.ControllerTestBase;
import com.odde.doughnut.controllers.dto.RecallStatsDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Regression guard: recall-stats aggregation must not regress to an N+1. With ~200 answered recalls
 * the endpoint used to issue one query per recall prompt (eager answer/predefined-question loading
 * on a native SELECT rp.*) and time out in production. The projection fix must keep the
 * prepared-statement count bounded regardless of history size.
 */
class RecallStatsPerformanceTest extends ControllerTestBase {
  @Autowired RecallStatsService recallStatsService;
  @Autowired EntityManager em;

  private User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
  }

  @Test
  void recallStatsStaysBoundedForLargeHistory() {
    // "now" at day 400 00:00; 5y window = [day -1425 -> clamped to day 0, day 400); 1y window =
    // [day 35 00:00, day 400).
    Timestamp now = makeMe.aTimestamp().of(400, 0).please();

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    MemoryTracker mt = makeMe.aMemoryTrackerFor(note).by(user).please();
    int count = 200;
    for (int i = 0; i < count; i++) {
      // days 35..234, hour 10 — all inside the 1y (and 5y) window.
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(mt)
          .answerChoiceIndex(0)
          .answerTimestamp(makeMe.aTimestamp().of(35 + i, 10).please())
          .please();
    }

    // Flush setup, then evict everything so compute() loads fresh entities (reproduces the N+1).
    em.flush();
    em.clear();
    Statistics stats = em.unwrap(Session.class).getFactory().getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();

    RecallStatsDTO dto = recallStatsService.compute(user, ZoneId.of("UTC"), now);

    long queries = stats.getPrepareStatementCount();
    assertThat("N+1 regression: query count must stay bounded", queries, lessThan(10L));
    assertThat(dto.getTotals().getTotalReviews365(), equalTo(count));
  }
}
