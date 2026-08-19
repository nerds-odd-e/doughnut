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
class RecallLogElapsedHoursBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void fillsFirstMappedNullElapsedAsZeroAndKeepsSchedule() throws Exception {
    MemoryTracker tracker = trackerWithSchedule();
    Timestamp due = tracker.getNextRecallAt();
    Timestamp lastRecalledAt = tracker.getLastRecalledAt();
    var log = makeMe.aRecallLogFor(tracker).please();

    runBackfill();

    FilledRow row = filledRow(log.getId(), tracker.getId());
    assertThat(row.elapsedHours(), equalTo(0));
    assertThat(row.stability(), equalTo(55f));
    assertThat(row.difficulty(), equalTo(5.3f));
    assertThat(row.nextRecallAt(), equalTo(due));
    assertThat(row.lastRecalledAt(), equalTo(lastRecalledAt));
  }

  @Test
  void fillsLaterMappedNullElapsedFromPreviousMapped() throws Exception {
    MemoryTracker tracker = trackerWithSchedule();
    Timestamp firstAt = makeMe.aTimestamp().of(1, 0).please();
    Timestamp laterAt = makeMe.aTimestamp().of(2, 0).please();
    makeMe.aRecallLogFor(tracker).recordedAt(firstAt).please();
    var later =
        makeMe
            .aRecallLogFor(tracker)
            .productOutcome(ProductOutcome.HARD)
            .recordedAt(laterAt)
            .please();

    runBackfill();

    assertThat(elapsedHours(later.getId()), equalTo(24));
  }

  @Test
  void fillsConfusionNullElapsedFromLastMappedWithoutBecomingAnchor() throws Exception {
    MemoryTracker tracker = trackerWithSchedule();
    Timestamp mappedAt = makeMe.aTimestamp().of(1, 0).please();
    Timestamp confusionAt = makeMe.aTimestamp().of(2, 0).please();
    Timestamp laterMappedAt = makeMe.aTimestamp().of(3, 0).please();
    makeMe.aRecallLogFor(tracker).recordedAt(mappedAt).please();
    var confusion =
        makeMe
            .aRecallLogFor(tracker)
            .productOutcome(ProductOutcome.CONFUSION)
            .recordedAt(confusionAt)
            .please();
    var laterMapped =
        makeMe
            .aRecallLogFor(tracker)
            .productOutcome(ProductOutcome.EASY)
            .recordedAt(laterMappedAt)
            .please();

    runBackfill();

    assertThat(elapsedHours(confusion.getId()), equalTo(24));
    assertThat(elapsedHours(laterMapped.getId()), equalTo(48));
  }

  @Test
  void fillsConfusionNullElapsedAsZeroWhenNoMappedGrade() throws Exception {
    var confusion =
        makeMe
            .aRecallLogFor(trackerWithSchedule())
            .productOutcome(ProductOutcome.CONFUSION)
            .please();

    runBackfill();

    assertThat(elapsedHours(confusion.getId()), equalTo(0));
  }

  @Test
  void leavesNonNullElapsedUnchanged() throws Exception {
    MemoryTracker tracker = trackerWithSchedule();
    Timestamp firstAt = makeMe.aTimestamp().of(1, 0).please();
    Timestamp laterAt = makeMe.aTimestamp().of(2, 0).please();
    var first = makeMe.aRecallLogFor(tracker).recordedAt(firstAt).elapsedHours(99).please();
    var later =
        makeMe
            .aRecallLogFor(tracker)
            .productOutcome(ProductOutcome.AGAIN)
            .recordedAt(laterAt)
            .elapsedHours(7)
            .please();

    runBackfill();

    assertThat(elapsedHours(first.getId()), equalTo(99));
    assertThat(elapsedHours(later.getId()), equalTo(7));
  }

  private MemoryTracker trackerWithSchedule() {
    return makeMe
        .aMemoryTrackerFor(makeMe.aNote().please())
        .stabilityAndNextRecallAt(55f)
        .difficulty(5.3f)
        .please();
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      RecallLogElapsedHoursBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private FilledRow filledRow(Integer logId, Integer trackerId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT rl.elapsed_hours, mt.stability, mt.difficulty, mt.next_recall_at, mt.last_recalled_at
        FROM recall_log rl
        JOIN memory_tracker mt ON mt.id = rl.memory_tracker_id
        WHERE rl.id = ? AND mt.id = ?
        """,
        (rs, rowNum) ->
            new FilledRow(
                rs.getObject("elapsed_hours", Integer.class),
                rs.getObject("stability", Float.class),
                rs.getObject("difficulty", Float.class),
                rs.getTimestamp("next_recall_at"),
                rs.getTimestamp("last_recalled_at")),
        logId,
        trackerId);
  }

  private Integer elapsedHours(Integer logId) {
    return jdbcTemplate.queryForObject(
        "SELECT elapsed_hours FROM recall_log WHERE id = ?", Integer.class, logId);
  }

  private record FilledRow(
      Integer elapsedHours,
      Float stability,
      Float difficulty,
      Timestamp nextRecallAt,
      Timestamp lastRecalledAt) {}
}
