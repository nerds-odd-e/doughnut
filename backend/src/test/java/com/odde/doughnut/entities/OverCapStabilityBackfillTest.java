package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
class OverCapStabilityBackfillTest {

  static final float OVER_CAP_STABILITY_HOURS = 1_800_600f;
  static final float UNDER_CAP_STABILITY_HOURS = 55f;

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void clampsOverCapStabilityAndRebuildsDue() throws Exception {
    MemoryTracker overCap = overCapTracker();
    underCapTracker();
    Float difficulty = overCap.getDifficulty();
    Timestamp last = overCap.getLastRecalledAt();

    runBackfill();

    TrackerRow row = trackerRow(overCap.getId());
    assertThat(row.stability(), equalTo(Fsrs.MAXIMUM_INTERVAL_HOURS));
    assertThat(
        row.nextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                last, Fsrs.intervalHours(Fsrs.MAXIMUM_INTERVAL_HOURS))));
    assertThat(row.difficulty(), equalTo(difficulty));
  }

  @Test
  void leavesUnderCapSiblingUntouched() throws Exception {
    overCapTracker();
    MemoryTracker underCap = underCapTracker();
    Float stability = underCap.getStability();
    Timestamp due = underCap.getNextRecallAt();

    runBackfill();

    TrackerRow row = trackerRow(underCap.getId());
    assertThat(row.stability(), equalTo(stability));
    assertThat(row.nextRecallAt(), equalTo(due));
  }

  @Test
  void runningTwiceStaysAtTheCap() throws Exception {
    MemoryTracker overCap = overCapTracker();

    runBackfill();
    runBackfill();

    assertThat(trackerRow(overCap.getId()).stability(), equalTo(Fsrs.MAXIMUM_INTERVAL_HOURS));
  }

  private MemoryTracker overCapTracker() {
    return makeMe
        .aMemoryTrackerFor(makeMe.aNote().please())
        .stabilityAndNextRecallAt(OVER_CAP_STABILITY_HOURS)
        .difficulty(5.3f)
        .please();
  }

  private MemoryTracker underCapTracker() {
    return makeMe
        .aMemoryTrackerFor(makeMe.aNote().please())
        .stabilityAndNextRecallAt(UNDER_CAP_STABILITY_HOURS)
        .please();
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      OverCapStabilityBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
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

  private record TrackerRow(Float stability, Float difficulty, Timestamp nextRecallAt) {}
}
