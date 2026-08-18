package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.SQLException;
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
class StillNewFirstRatingBackfillTest {

  static final float FIRST_AGAIN_STABILITY_HOURS = 5f;
  static final float FIRST_AGAIN_DIFFICULTY = 6.4133f;
  static final float FIRST_HARD_STABILITY_HOURS = 31f;
  static final float FIRST_HARD_DIFFICULTY = 5.1121707f;

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @ParameterizedTest
  @EnumSource(
      value = ProductOutcome.class,
      names = {"AGAIN", "AGAIN_ZERO"})
  void runAgain_appliesAgainFirstRatingToStillNewTrackersWithAgainLogs(ProductOutcome outcome)
      throws Exception {
    MemoryTracker tracker = stillNewWith(outcome, makeMe.aTimestamp().of(1, 0).please());

    runAgain("1=1");

    assertAgainFirstRating(tracker.getId());
  }

  @Test
  void runAgain_leavesAssimilateOnlyTrackersNew() throws Exception {
    MemoryTracker assimilateOnly = stillNew(makeMe.aTimestamp().of(1, 0).please());

    runAgain("1=1");

    assertStillNew(assimilateOnly.getId(), assimilateOnly.getLastRecalledAt());
  }

  @Test
  void runAgain_leavesAlreadyGradedTrackersUnchanged() throws Exception {
    MemoryTracker alreadyGraded = gradedWithAgainLog(makeMe.aTimestamp().of(1, 0).please());
    Float gradedStability = alreadyGraded.getStability();

    runAgain("1=1");

    assertThat(stability(alreadyGraded.getId()), equalTo(gradedStability));
  }

  @Test
  void runAgain_leavesShrinkOnlyTrackersNew() throws Exception {
    MemoryTracker shrinkOnly =
        stillNewWith(ProductOutcome.SHRINK, makeMe.aTimestamp().of(1, 0).please());

    runAgain("1=1");

    assertStillNew(shrinkOnly.getId(), shrinkOnly.getLastRecalledAt());
  }

  @Test
  void runAgain_isNoOpWhenGateDisabled() throws Exception {
    MemoryTracker again = stillNewWith(ProductOutcome.AGAIN, makeMe.aTimestamp().of(2, 0).please());

    runAgain("1=0");

    assertStillNew(again.getId(), again.getLastRecalledAt());
  }

  @Test
  void runAgain_failsLoudWhenGateIsNeitherEnabledNorDisabled() {
    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> runAgain("typo"));

    assertThat(thrown.getMessage(), containsString("typo"));
  }

  @Test
  void runHard_appliesHardFirstRatingToStillNewTrackersWithShrinkLogs() throws Exception {
    MemoryTracker tracker =
        stillNewWith(ProductOutcome.SHRINK, makeMe.aTimestamp().of(1, 0).please());

    runHard("1=1");

    assertHardFirstRating(tracker.getId());
  }

  @Test
  void runHard_leavesAgainAlreadyMigratedTrackersUnchanged() throws Exception {
    MemoryTracker tracker =
        stillNewWith(ProductOutcome.AGAIN, makeMe.aTimestamp().of(1, 0).please());
    runAgain("1=1");
    Float afterAgain = stability(tracker.getId());

    runHard("1=1");

    assertThat(stability(tracker.getId()), equalTo(afterAgain));
  }

  @Test
  void runHard_isNoOpWhenGateDisabled() throws Exception {
    MemoryTracker shrink =
        stillNewWith(ProductOutcome.SHRINK, makeMe.aTimestamp().of(2, 0).please());

    runHard("1=0");

    assertStillNew(shrink.getId(), shrink.getLastRecalledAt());
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

  private void assertAgainFirstRating(Integer id) {
    assertFirstRating(id, FIRST_AGAIN_STABILITY_HOURS, FIRST_AGAIN_DIFFICULTY);
  }

  private void assertHardFirstRating(Integer id) {
    assertFirstRating(id, FIRST_HARD_STABILITY_HOURS, FIRST_HARD_DIFFICULTY);
  }

  private void assertFirstRating(Integer id, float expectedStability, float expectedDifficulty) {
    TrackerRow row = trackerRow(id);
    assertThat(row.stability(), equalTo(expectedStability));
    assertThat((double) row.difficulty(), closeTo(expectedDifficulty, 1e-5));
    assertThat(
        row.nextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                row.lastRecalledAt(), Math.round(expectedStability))));
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

  @FunctionalInterface
  private interface BackfillRun {
    void run(Connection connection, String gate) throws SQLException;
  }

  private record TrackerRow(
      Float stability, Float difficulty, Timestamp nextRecallAt, Timestamp lastRecalledAt) {}
}
