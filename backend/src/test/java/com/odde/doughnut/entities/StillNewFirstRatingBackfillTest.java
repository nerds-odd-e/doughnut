package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.testability.MakeMe;
import java.sql.Connection;
import java.sql.SQLException;
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
class StillNewFirstRatingBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void runAgain_isNoOpWhenGateDisabled() throws Exception {
    MemoryTracker again = stillNewWith(ProductOutcome.AGAIN, makeMe.aTimestamp().of(2, 0).please());

    runAgain("1=0");

    assertStillNew(again.getId(), again.getAssimilatedAt());
  }

  @Test
  void runAgain_failsLoudWhenGateIsNeitherEnabledNorDisabled() {
    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> runAgain("typo"));

    assertThat(thrown.getMessage(), containsString("typo"));
  }

  @Test
  void runHard_isNoOpWhenGateDisabled() throws Exception {
    MemoryTracker shrinkColumnLiteral =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(makeMe.aTimestamp().of(2, 0).please())
            .please();
    setRecallLogProductOutcome(
        makeMe.aRecallLogFor(shrinkColumnLiteral).please().getId(), "SHRINK");

    runHard("1=0");

    assertStillNew(shrinkColumnLiteral.getId(), shrinkColumnLiteral.getAssimilatedAt());
  }

  private MemoryTracker stillNewWith(ProductOutcome outcome, Timestamp assimilatedAt) {
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(makeMe.aNote().please()).assimilatedAt(assimilatedAt).please();
    makeMe.aRecallLogFor(tracker).productOutcome(outcome).please();
    return tracker;
  }

  private void setRecallLogProductOutcome(Integer logId, String productOutcome) {
    makeMe.entityPersister.flushAndClear();
    jdbcTemplate.update(
        "UPDATE recall_log SET product_outcome = ? WHERE id = ?", productOutcome, logId);
  }

  private void runAgain(String gate) throws Exception {
    runBackfill(StillNewFirstRatingBackfill::runAgain, gate);
  }

  private void runHard(String gate) throws Exception {
    runBackfill(StillNewFirstRatingBackfill::runHard, gate);
  }

  private void runBackfill(BackfillRun backfill, String gate) throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      backfill.run(connection, gate);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void assertStillNew(Integer id, Timestamp expectedDue) {
    TrackerRow row = trackerRow(id);
    assertThat(row.stability(), equalTo(ForgettingCurve.ASSIMILATE_STABILITY_HOURS));
    assertThat(row.difficulty(), nullValue());
    assertThat(row.nextRecallAt(), equalTo(expectedDue));
  }

  private TrackerRow trackerRow(Integer id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT stability, difficulty, next_recall_at
        FROM memory_tracker WHERE id = ?
        """,
        (rs, rowNum) ->
            new TrackerRow(
                rs.getObject("stability", Float.class),
                rs.getObject("difficulty", Float.class),
                rs.getTimestamp("next_recall_at")),
        id);
  }

  @FunctionalInterface
  private interface BackfillRun {
    void run(Connection connection, String gate) throws SQLException;
  }

  private record TrackerRow(Float stability, Float difficulty, Timestamp nextRecallAt) {}
}
