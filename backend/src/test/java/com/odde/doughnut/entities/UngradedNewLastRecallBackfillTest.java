package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
class UngradedNewLastRecallBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void nullsLastRecallForAssimilateOnlyNewAndKeepsDueAtAssimilated() throws Exception {
    MemoryTracker assimilateOnly = ungradedNew(makeMe.aTimestamp().of(1, 0).please());

    runBackfill();

    TrackerRow row = trackerRow(assimilateOnly.getId());
    assertThat(row.lastRecalledAt(), nullValue());
    assertThat(row.nextRecallAt(), equalTo(assimilateOnly.getAssimilatedAt()));
  }

  @Test
  void nullsLastRecallForRemovedNewWithNoMappedLogs() throws Exception {
    Timestamp lastRecalled = makeMe.aTimestamp().of(1, 0).please();
    MemoryTracker removed =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(lastRecalled)
            .lastRecalledAt(lastRecalled)
            .removedFromTracking()
            .please();

    runBackfill();

    assertThat(trackerRow(removed.getId()).lastRecalledAt(), nullValue());
  }

  @Test
  void nullsLastRecallForConfusionOnlyNew() throws Exception {
    MemoryTracker confusionOnly =
        ungradedNewWith(ProductOutcome.CONFUSION, makeMe.aTimestamp().of(1, 0).please());

    runBackfill();

    assertThat(trackerRow(confusionOnly.getId()).lastRecalledAt(), nullValue());
  }

  @Test
  void leavesStillNewWithMappedLogsUnchanged() throws Exception {
    Timestamp lastRecalled = makeMe.aTimestamp().of(1, 0).please();
    MemoryTracker stillNewWithMappedLog = ungradedNewWith(ProductOutcome.AGAIN, lastRecalled);

    runBackfill();

    assertThat(trackerRow(stillNewWithMappedLog.getId()).lastRecalledAt(), equalTo(lastRecalled));
  }

  @Test
  void leavesGradedTrackersUnchanged() throws Exception {
    MemoryTracker graded =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .stabilityAndNextRecallAt(55f)
            .difficulty(5.3f)
            .please();
    Timestamp lastRecalled = graded.getLastRecalledAt();

    runBackfill();

    assertThat(trackerRow(graded.getId()).lastRecalledAt(), equalTo(lastRecalled));
  }

  private MemoryTracker ungradedNewWith(ProductOutcome outcome, Timestamp lastRecalled) {
    MemoryTracker tracker = ungradedNew(lastRecalled);
    makeMe.aRecallLogFor(tracker).productOutcome(outcome).please();
    return tracker;
  }

  private MemoryTracker ungradedNew(Timestamp lastRecalled) {
    return makeMe
        .aMemoryTrackerFor(makeMe.aNote().please())
        .assimilatedAt(lastRecalled)
        .lastRecalledAt(lastRecalled)
        .please();
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      UngradedNewLastRecallBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private TrackerRow trackerRow(Integer id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT last_recalled_at, next_recall_at
        FROM memory_tracker WHERE id = ?
        """,
        (rs, rowNum) ->
            new TrackerRow(rs.getTimestamp("last_recalled_at"), rs.getTimestamp("next_recall_at")),
        id);
  }

  private record TrackerRow(Timestamp lastRecalledAt, Timestamp nextRecallAt) {}
}
