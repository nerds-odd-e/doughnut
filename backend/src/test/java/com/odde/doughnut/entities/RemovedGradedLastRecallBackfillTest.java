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
class RemovedGradedLastRecallBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void setsLastRecallToLatestMappedGradeAndKeepsDue() throws Exception {
    Timestamp gradeTime = makeMe.aTimestamp().of(1, 0).please();
    Timestamp removeBump = makeMe.aTimestamp().of(2, 0).please();
    Timestamp due = makeMe.aTimestamp().of(9, 0).please();
    MemoryTracker removed =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(gradeTime)
            .stabilityAndNextRecallAt(55f)
            .difficulty(5.3f)
            .lastRecalledAt(removeBump)
            .nextRecallAt(due)
            .removedFromTracking()
            .please();
    makeMe
        .aRecallLogFor(removed)
        .productOutcome(ProductOutcome.GOOD)
        .recordedAt(gradeTime)
        .please();

    runBackfill();

    TrackerRow row = trackerRow(removed.getId());
    assertThat(row.lastRecalledAt(), equalTo(gradeTime));
    assertThat(row.nextRecallAt(), equalTo(due));
  }

  @Test
  void setsLastRecallToTheLaterMappedLog() throws Exception {
    Timestamp earlier = makeMe.aTimestamp().of(1, 0).please();
    Timestamp later = makeMe.aTimestamp().of(2, 0).please();
    Timestamp removeBump = makeMe.aTimestamp().of(3, 0).please();
    MemoryTracker removed = removedGraded(ProductOutcome.GOOD, earlier, removeBump);
    makeMe.aRecallLogFor(removed).productOutcome(ProductOutcome.HARD).recordedAt(later).please();

    runBackfill();

    assertThat(trackerRow(removed.getId()).lastRecalledAt(), equalTo(later));
  }

  @Test
  void leavesNotRemovedTrackerUnchanged() throws Exception {
    Timestamp gradeTime = makeMe.aTimestamp().of(1, 0).please();
    Timestamp bump = makeMe.aTimestamp().of(2, 0).please();
    MemoryTracker stillTracking = graded(ProductOutcome.GOOD, gradeTime, bump);

    runBackfill();

    assertThat(trackerRow(stillTracking.getId()).lastRecalledAt(), equalTo(bump));
  }

  @Test
  void leavesRemovedNewWithNoMappedLogsNull() throws Exception {
    MemoryTracker removedNew =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(makeMe.aTimestamp().of(1, 0).please())
            .removedFromTracking()
            .please();

    runBackfill();

    assertThat(trackerRow(removedNew.getId()).lastRecalledAt(), nullValue());
  }

  @Test
  void leavesConfusionOnlyRemovedLastRecallNull() throws Exception {
    Timestamp assimilated = makeMe.aTimestamp().of(1, 0).please();
    MemoryTracker confusionOnly =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(assimilated)
            .removedFromTracking()
            .please();
    makeMe
        .aRecallLogFor(confusionOnly)
        .productOutcome(ProductOutcome.CONFUSION)
        .recordedAt(assimilated)
        .please();

    runBackfill();

    assertThat(trackerRow(confusionOnly.getId()).lastRecalledAt(), nullValue());
  }

  private MemoryTracker removedGraded(
      ProductOutcome outcome, Timestamp gradeTime, Timestamp lastRecalledAt) {
    return graded(outcome, gradeTime, lastRecalledAt, true);
  }

  private MemoryTracker graded(
      ProductOutcome outcome, Timestamp gradeTime, Timestamp lastRecalledAt) {
    return graded(outcome, gradeTime, lastRecalledAt, false);
  }

  private MemoryTracker graded(
      ProductOutcome outcome, Timestamp gradeTime, Timestamp lastRecalledAt, boolean removed) {
    var builder =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(gradeTime)
            .stabilityAndNextRecallAt(55f)
            .difficulty(5.3f)
            .lastRecalledAt(lastRecalledAt);
    if (removed) {
      builder.removedFromTracking();
    }
    MemoryTracker tracker = builder.please();
    makeMe.aRecallLogFor(tracker).productOutcome(outcome).recordedAt(gradeTime).please();
    return tracker;
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      RemovedGradedLastRecallBackfill.run(connection);
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
