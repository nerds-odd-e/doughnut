package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

import com.odde.doughnut.entities.RecallLogElapsedHoursBackfill.LogRow;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
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
  void reconstructsFirstMappedNullElapsedAsZero() {
    Timestamp firstAt = makeMe.aTimestamp().of(1, 0).please();

    assertThat(reconstructed(nullElapsed(1, firstAt, ProductOutcome.GOOD)), hasEntry(1, 0));
  }

  @Test
  void reconstructsLaterMappedNullElapsedFromPreviousMapped() {
    Timestamp firstAt = makeMe.aTimestamp().of(1, 0).please();
    Timestamp laterAt = makeMe.aTimestamp().of(2, 0).please();

    assertThat(
        reconstructed(
            nullElapsed(1, firstAt, ProductOutcome.GOOD),
            nullElapsed(2, laterAt, ProductOutcome.HARD)),
        hasEntry(2, 24));
  }

  @Test
  void reconstructsConfusionNullElapsedFromLastMappedWithoutBecomingAnchor() {
    Timestamp mappedAt = makeMe.aTimestamp().of(1, 0).please();
    Timestamp confusionAt = makeMe.aTimestamp().of(2, 0).please();
    Timestamp laterMappedAt = makeMe.aTimestamp().of(3, 0).please();

    Map<Integer, Integer> updates =
        reconstructed(
            nullElapsed(1, mappedAt, ProductOutcome.GOOD),
            nullElapsed(2, confusionAt, ProductOutcome.CONFUSION),
            nullElapsed(3, laterMappedAt, ProductOutcome.EASY));

    assertThat(updates, hasEntry(2, 24));
    assertThat(updates, hasEntry(3, 48));
  }

  @Test
  void reconstructsConfusionNullElapsedAsZeroWhenNoMappedGrade() {
    Timestamp confusionAt = makeMe.aTimestamp().of(1, 0).please();

    assertThat(
        reconstructed(nullElapsed(1, confusionAt, ProductOutcome.CONFUSION)), hasEntry(1, 0));
  }

  @Test
  void persistedLogDefaultsElapsedHoursToZero() {
    var log =
        makeMe.aRecallLogFor(makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please()).please();
    makeMe.entityPersister.flush();

    assertThat(elapsedHours(log.getId()), equalTo(0));
  }

  @Test
  void leavesPersistedElapsedAndScheduleUnchanged() throws Exception {
    MemoryTracker tracker = trackerWithSchedule();
    Timestamp due = tracker.getNextRecallAt();
    Timestamp lastRecalledAt = tracker.getLastRecalledAt();
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

    FilledRow row = filledRow(first.getId(), tracker.getId());
    assertThat(row.elapsedHours(), equalTo(99));
    assertThat(elapsedHours(later.getId()), equalTo(7));
    assertThat(row.stability(), equalTo(55f));
    assertThat(row.difficulty(), equalTo(5.3f));
    assertThat(row.nextRecallAt(), equalTo(due));
    assertThat(row.lastRecalledAt(), equalTo(lastRecalledAt));
  }

  private static LogRow nullElapsed(int id, Timestamp recordedAt, ProductOutcome outcome) {
    return new LogRow(id, 10, recordedAt, outcome, null);
  }

  private static Map<Integer, Integer> reconstructed(LogRow... rows) {
    return RecallLogElapsedHoursBackfill.reconstructedNullElapsedById(List.of(rows));
  }

  private MemoryTracker trackerWithSchedule() {
    return makeMe
        .aMemoryTrackerFor(makeMe.aNote().please())
        .stabilityAndNextRecallAt(55f)
        .difficulty(5.3f)
        .please();
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flushAndClear();
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
                rs.getInt("elapsed_hours"),
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
      int elapsedHours,
      Float stability,
      Float difficulty,
      Timestamp nextRecallAt,
      Timestamp lastRecalledAt) {}
}
