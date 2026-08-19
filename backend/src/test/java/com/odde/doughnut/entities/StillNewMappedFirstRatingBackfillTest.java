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
class StillNewMappedFirstRatingBackfillTest {

  static final float FIRST_AGAIN_STABILITY_HOURS = 5f;
  static final float FIRST_AGAIN_DIFFICULTY = 6.4133f;
  static final float FIRST_HARD_STABILITY_HOURS = 31f;
  static final float FIRST_HARD_DIFFICULTY = 5.1121707f;
  static final float FIRST_GOOD_STABILITY_HOURS = 55f;
  static final float FIRST_GOOD_DIFFICULTY = 2.118104f;

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void appliesAgainFirstRatingFromMappedLogTime() throws Exception {
    Timestamp assimilateBump = makeMe.aTimestamp().of(1, 0).please();
    Timestamp gradeTime = makeMe.aTimestamp().of(2, 0).please();
    MemoryTracker tracker = stillNewWith(ProductOutcome.AGAIN, assimilateBump, gradeTime);

    runBackfill();

    TrackerRow row = trackerRow(tracker.getId());
    assertThat(row.stability(), equalTo(FIRST_AGAIN_STABILITY_HOURS));
    assertThat((double) row.difficulty(), closeTo(FIRST_AGAIN_DIFFICULTY, 1e-5));
    assertThat(row.lastRecalledAt(), equalTo(gradeTime));
    assertThat(
        row.nextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                gradeTime, Math.round(FIRST_AGAIN_STABILITY_HOURS))));
  }

  @Test
  void appliesHardFirstRatingToShrinkOnly() throws Exception {
    Timestamp gradeTime = makeMe.aTimestamp().of(2, 0).please();
    MemoryTracker tracker = stillNew(makeMe.aTimestamp().of(1, 0).please());
    setRecallLogProductOutcome(
        makeMe.aRecallLogFor(tracker).recordedAt(gradeTime).please().getId(), "SHRINK");

    runBackfill();

    TrackerRow row = trackerRow(tracker.getId());
    assertThat(row.stability(), equalTo(FIRST_HARD_STABILITY_HOURS));
    assertThat((double) row.difficulty(), closeTo(FIRST_HARD_DIFFICULTY, 1e-5));
  }

  @Test
  void mixedAgainAndShrinkKeepsAgainFirstRatingAndLatestMappedLastRecall() throws Exception {
    Timestamp assimilateBump = makeMe.aTimestamp().of(1, 0).please();
    Timestamp againAt = makeMe.aTimestamp().of(2, 0).please();
    Timestamp shrinkAt = makeMe.aTimestamp().of(3, 0).please();
    MemoryTracker tracker = stillNew(assimilateBump);
    makeMe.aRecallLogFor(tracker).productOutcome(ProductOutcome.AGAIN).recordedAt(againAt).please();
    setRecallLogProductOutcome(
        makeMe.aRecallLogFor(tracker).recordedAt(shrinkAt).please().getId(), "SHRINK");

    runBackfill();

    TrackerRow row = trackerRow(tracker.getId());
    assertThat(row.stability(), equalTo(FIRST_AGAIN_STABILITY_HOURS));
    assertThat(row.lastRecalledAt(), equalTo(shrinkAt));
  }

  @Test
  void appliesGoodFirstRatingToGoodOnly() throws Exception {
    MemoryTracker tracker =
        stillNewWith(
            ProductOutcome.GOOD,
            makeMe.aTimestamp().of(1, 0).please(),
            makeMe.aTimestamp().of(2, 0).please());

    runBackfill();

    TrackerRow row = trackerRow(tracker.getId());
    assertThat(row.stability(), equalTo(FIRST_GOOD_STABILITY_HOURS));
    assertThat((double) row.difficulty(), closeTo(FIRST_GOOD_DIFFICULTY, 1e-5));
  }

  @Test
  void leavesAlreadyGradedTrackersUnchanged() throws Exception {
    MemoryTracker alreadyGraded =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .stabilityAndNextRecallAt(55f)
            .difficulty(5.3f)
            .please();
    makeMe.aRecallLogFor(alreadyGraded).productOutcome(ProductOutcome.AGAIN).please();
    Float gradedStability = alreadyGraded.getStability();

    runBackfill();

    assertThat(trackerRow(alreadyGraded.getId()).stability(), equalTo(gradedStability));
  }

  @Test
  void leavesAssimilateOnlyTrackersNew() throws Exception {
    MemoryTracker assimilateOnly = stillNew(makeMe.aTimestamp().of(1, 0).please());

    runBackfill();

    TrackerRow row = trackerRow(assimilateOnly.getId());
    assertThat(row.stability(), equalTo(Fsrs.NEW_STABILITY_HOURS));
    assertThat(row.difficulty(), nullValue());
  }

  private MemoryTracker stillNewWith(
      ProductOutcome outcome, Timestamp assimilateBump, Timestamp gradeTime) {
    MemoryTracker tracker = stillNew(assimilateBump);
    makeMe.aRecallLogFor(tracker).productOutcome(outcome).recordedAt(gradeTime).please();
    return tracker;
  }

  private void setRecallLogProductOutcome(Integer logId, String productOutcome) {
    makeMe.entityPersister.flushAndClear();
    jdbcTemplate.update(
        "UPDATE recall_log SET product_outcome = ? WHERE id = ?", productOutcome, logId);
  }

  private MemoryTracker stillNew(Timestamp assimilateBump) {
    return makeMe
        .aMemoryTrackerFor(makeMe.aNote().please())
        .assimilatedAt(assimilateBump)
        .lastRecalledAt(assimilateBump)
        .please();
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      StillNewMappedFirstRatingBackfill.run(connection);
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
