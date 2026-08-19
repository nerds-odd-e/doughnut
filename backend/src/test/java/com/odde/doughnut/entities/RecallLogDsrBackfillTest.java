package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
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
class RecallLogDsrBackfillTest {

  static final float FIRST_GOOD_STABILITY_HOURS = 55f;
  static final float FIRST_GOOD_DIFFICULTY = 2.118104f;
  static final float LEFTOVER_STABILITY_HOURS = 100f;
  static final float LEFTOVER_DIFFICULTY = 5.3f;

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void rebuildsLeftoverFirstMappedGoodAsFirstRating() throws Exception {
    Timestamp leftoverLast = makeMe.aTimestamp().of(1, 0).please();
    Timestamp gradeTime = makeMe.aTimestamp().of(2, 0).please();
    MemoryTracker leftover = leftoverFirstMappedGood(leftoverLast, gradeTime);

    runBackfill();

    TrackerRow row = trackerRow(leftover.getId());
    assertThat(row.stability(), equalTo(FIRST_GOOD_STABILITY_HOURS));
    assertThat((double) row.difficulty(), closeTo(FIRST_GOOD_DIFFICULTY, 1e-5));
    assertThat(row.lastRecalledAt(), equalTo(gradeTime));
    assertThat(
        row.nextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                gradeTime, Fsrs.intervalHours(FIRST_GOOD_STABILITY_HOURS))));
  }

  @Test
  void leavesNewWithNoMappedGradeUnchanged() throws Exception {
    leftoverFirstMappedGood(
        makeMe.aTimestamp().of(1, 0).please(), makeMe.aTimestamp().of(2, 0).please());
    MemoryTracker ungradedNew = makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please();

    runBackfill();

    TrackerRow row = trackerRow(ungradedNew.getId());
    assertThat(row.stability(), equalTo(Fsrs.NEW_STABILITY_HOURS));
    assertThat(row.difficulty(), nullValue());
  }

  private MemoryTracker leftoverFirstMappedGood(Timestamp leftoverLast, Timestamp gradeTime) {
    MemoryTracker leftover =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .stabilityAndNextRecallAt(LEFTOVER_STABILITY_HOURS)
            .difficulty(LEFTOVER_DIFFICULTY)
            .lastRecalledAt(leftoverLast)
            .please();
    makeMe
        .aRecallLogFor(leftover)
        .productOutcome(ProductOutcome.GOOD)
        .recordedAt(gradeTime)
        .elapsedHours(0)
        .please();
    return leftover;
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      RecallLogDsrBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private TrackerRow trackerRow(Integer id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT stability, difficulty, next_recall_at, last_recalled_at
        FROM memory_tracker WHERE id = ?
        """,
        (rs, rowNum) ->
            new TrackerRow(
                rs.getObject("stability", Float.class),
                rs.getObject("difficulty", Float.class),
                rs.getTimestamp("next_recall_at"),
                rs.getTimestamp("last_recalled_at")),
        id);
  }

  private record TrackerRow(
      Float stability, Float difficulty, Timestamp nextRecallAt, Timestamp lastRecalledAt) {}
}
