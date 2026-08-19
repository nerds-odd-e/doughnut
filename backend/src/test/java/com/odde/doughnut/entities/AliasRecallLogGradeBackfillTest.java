package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.testability.MakeMe;
import java.sql.Connection;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AliasRecallLogGradeBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void rewritesShrinkAliasToHardAndKeepsSchedule() throws Exception {
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .stabilityAndNextRecallAt(55f)
            .difficulty(5.3f)
            .please();
    Timestamp due = tracker.getNextRecallAt();
    var log = makeMe.aRecallLogFor(tracker).please();
    setRecallLogProductOutcome(log.getId(), "SHRINK");

    runBackfill();

    RewrittenRow row = rewrittenRow(log.getId(), tracker.getId());
    assertThat(row.productOutcome(), equalTo("HARD"));
    assertThat(row.stability(), equalTo(55f));
    assertThat(row.difficulty(), equalTo(5.3f));
    assertThat(row.nextRecallAt(), equalTo(due));
  }

  @Test
  void rewritesAgainZeroAliasToAgain() throws Exception {
    var log =
        makeMe.aRecallLogFor(makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please()).please();
    setRecallLogProductOutcome(log.getId(), "AGAIN_ZERO");

    runBackfill();

    assertThat(productOutcome(log.getId()), equalTo("AGAIN"));
  }

  @Test
  void leavesGoodRecallLogUnchanged() throws Exception {
    var log =
        makeMe
            .aRecallLogFor(makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please())
            .productOutcome(ProductOutcome.GOOD)
            .please();

    runBackfill();

    assertThat(productOutcome(log.getId()), equalTo("GOOD"));
  }

  private void setRecallLogProductOutcome(Integer logId, String productOutcome) {
    makeMe.entityPersister.flushAndClear();
    jdbcTemplate.update(
        "UPDATE recall_log SET product_outcome = ? WHERE id = ?", productOutcome, logId);
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      AliasRecallLogGradeBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private RewrittenRow rewrittenRow(Integer logId, Integer trackerId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT rl.product_outcome, mt.stability, mt.difficulty, mt.next_recall_at
        FROM recall_log rl
        JOIN memory_tracker mt ON mt.id = rl.memory_tracker_id
        WHERE rl.id = ? AND mt.id = ?
        """,
        (rs, rowNum) ->
            new RewrittenRow(
                rs.getString("product_outcome"),
                rs.getObject("stability", Float.class),
                rs.getObject("difficulty", Float.class),
                rs.getTimestamp("next_recall_at")),
        logId,
        trackerId);
  }

  private String productOutcome(Integer logId) {
    return jdbcTemplate.queryForObject(
        "SELECT product_outcome FROM recall_log WHERE id = ?", String.class, logId);
  }

  private record RewrittenRow(
      String productOutcome, Float stability, Float difficulty, Timestamp nextRecallAt) {}
}
