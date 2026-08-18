package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StillNewAgainFirstRatingBackfillTest {

  static final float FIRST_AGAIN_STABILITY_HOURS = 5f;
  static final float FIRST_AGAIN_DIFFICULTY = 6.4133f;

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @ParameterizedTest
  @EnumSource(
      value = ProductOutcome.class,
      names = {"AGAIN", "AGAIN_ZERO"})
  void run_appliesAgainFirstRatingToStillNewTrackersWithAgainLogs(ProductOutcome outcome)
      throws Exception {
    MemoryTracker tracker = stillNewWith(outcome, makeMe.aTimestamp().of(1, 0).please());

    runBackfill("1=1");

    assertAgainFirstRating(tracker.getId());
  }

  @Test
  void run_leavesAssimilateOnlyTrackersNew() throws Exception {
    MemoryTracker assimilateOnly = stillNew(makeMe.aTimestamp().of(1, 0).please());

    runBackfill("1=1");

    assertStillNew(assimilateOnly.getId(), assimilateOnly.getLastRecalledAt());
  }

  @Test
  void run_leavesAlreadyGradedTrackersUnchanged() throws Exception {
    MemoryTracker alreadyGraded = gradedWithAgainLog(makeMe.aTimestamp().of(1, 0).please());
    Float gradedStability = alreadyGraded.getStability();

    runBackfill("1=1");

    assertThat(stability(alreadyGraded.getId()), equalTo(gradedStability));
  }

  @Test
  void run_leavesShrinkOnlyTrackersNew() throws Exception {
    MemoryTracker shrinkOnly =
        stillNewWith(ProductOutcome.SHRINK, makeMe.aTimestamp().of(1, 0).please());

    runBackfill("1=1");

    assertStillNew(shrinkOnly.getId(), shrinkOnly.getLastRecalledAt());
  }

  @Test
  void run_isNoOpWhenGateDisabled() throws Exception {
    MemoryTracker again = stillNewWith(ProductOutcome.AGAIN, makeMe.aTimestamp().of(2, 0).please());

    runBackfill("1=0");

    assertStillNew(again.getId(), again.getLastRecalledAt());
  }

  @Test
  void run_failsLoudWhenGateIsNeitherEnabledNorDisabled() {
    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> runBackfill("typo"));

    assertThat(thrown.getMessage(), containsString("typo"));
  }

  private MemoryTracker stillNewWith(ProductOutcome outcome, Timestamp lastRecalled) {
    MemoryTracker tracker = stillNew(lastRecalled);
    makeMe.aRecallLogFor(tracker).productOutcome(outcome).please();
    return tracker;
  }

  private MemoryTracker stillNew(Timestamp lastRecalled) {
    return makeMe.aMemoryTrackerFor(makeMe.aNote().please()).assimilatedAt(lastRecalled).please();
  }

  private MemoryTracker gradedWithAgainLog(Timestamp lastRecalled) {
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .assimilatedAt(lastRecalled)
            .stabilityAndNextRecallAt(55f)
            .difficulty(5.3f)
            .please();
    makeMe.aRecallLogFor(tracker).productOutcome(ProductOutcome.AGAIN).please();
    return tracker;
  }

  private void runBackfill(String gate) throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      StillNewAgainFirstRatingBackfill.run(connection, gate);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void assertAgainFirstRating(Integer id) {
    TrackerRow row = trackerRow(id);
    assertThat(row.stability(), equalTo(FIRST_AGAIN_STABILITY_HOURS));
    assertThat(row.difficulty(), equalTo(FIRST_AGAIN_DIFFICULTY));
    assertThat(
        row.nextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                row.lastRecalledAt(), Math.round(FIRST_AGAIN_STABILITY_HOURS))));
  }

  private void assertStillNew(Integer id, Timestamp lastRecalled) {
    TrackerRow row = trackerRow(id);
    assertThat(row.stability(), equalTo(ForgettingCurve.ASSIMILATE_STABILITY_HOURS));
    assertThat(row.difficulty(), nullValue());
    assertThat(row.nextRecallAt(), equalTo(lastRecalled));
  }

  private Float stability(Integer id) {
    return trackerRow(id).stability();
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
