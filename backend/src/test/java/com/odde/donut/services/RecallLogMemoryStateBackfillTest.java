package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the backfill recovers exactly what the live path (slice 15/16's {@code persistRecallLog})
 * would have recorded: grade and confusion history are created for real through {@link
 * MemoryTrackerService}, the live-captured values are stashed away, the columns are nulled out to
 * simulate pre-instrumentation rows, and the backfill is asserted to reproduce the stashed values
 * exactly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecallLogMemoryStateBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired RecallLogRepository recallLogRepository;
  @Autowired EntityManager entityManager;

  private Note note;

  @BeforeEach
  void setUp() {
    User owner = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(owner).please();
  }

  private record Snapshot(Float stabilityBefore, Float difficultyBefore, Double retrievability) {
    static Snapshot of(RecallLog log) {
      return new Snapshot(
          log.getStabilityBefore(), log.getDifficultyBefore(), log.getRetrievability());
    }
  }

  private List<RecallLog> orderedLogsFor(MemoryTracker tracker) {
    return recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtAscIdAsc(tracker.getId());
  }

  private List<Snapshot> nullOutMemoryStateColumns(List<RecallLog> logs) {
    List<Snapshot> snapshots = logs.stream().map(Snapshot::of).toList();
    for (RecallLog log : logs) {
      log.setStabilityBefore(null);
      log.setDifficultyBefore(null);
      log.setRetrievability(null);
    }
    recallLogRepository.saveAll(logs);
    entityManager.flush();
    entityManager.clear();
    return snapshots;
  }

  @Test
  void recoversLiveValuesForGradedHistory() {
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).please(); // New tracker
    Timestamp t1 = makeMe.aTimestamp().of(1, 1).please();
    memoryTrackerService.markAsRecalled(t1, Grade.GOOD, tracker, null, null);
    Timestamp t2 = makeMe.aTimestamp().of(3, 1).please();
    memoryTrackerService.markAsRecalled(t2, Grade.HARD, tracker, null, null);
    entityManager.flush();

    List<RecallLog> liveLogs = orderedLogsFor(tracker);
    assertThat(liveLogs, hasSize(2));
    // Sanity: the New-tracker first grade has no prior difficulty/retrievability (slice 16).
    assertThat(liveLogs.get(0).getStabilityBefore(), equalTo(0f));
    assertThat(liveLogs.get(0).getRetrievability(), nullValue());
    List<Snapshot> expected = nullOutMemoryStateColumns(liveLogs);

    RecallLogMemoryStateBackfill.Result result =
        RecallLogMemoryStateBackfill.run(recallLogRepository);

    assertThat(result.rowsBackfilled(), is(2));
    List<RecallLog> backfilled = orderedLogsFor(tracker);
    for (int i = 0; i < backfilled.size(); i++) {
      assertThat(Snapshot.of(backfilled.get(i)), equalTo(expected.get(i)));
    }
  }

  @Test
  void recoversLiveValuesForConfusionHistory() {
    // A confusion adjustment can land on a tracker that has never been graded yet (e.g. an
    // accidental-match spelling confusion hitting a note-level tracker before its first
    // recall) — every real tracker starts New (see MemoryTrackerAssimilation), so this is the
    // realistic shape of a first-ever recall_log row being a CONFUSION row.
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).please();
    Timestamp t1 = makeMe.aTimestamp().of(1, 1).please();
    memoryTrackerService.persistRecallLog(tracker, t1, null, null, null);
    memoryTrackerService.applyConfusionAdjustment(tracker, t1);
    entityManager.flush();

    List<RecallLog> liveLogs = orderedLogsFor(tracker);
    assertThat(liveLogs, hasSize(1));
    assertThat(liveLogs.get(0).isConfusion(), is(true));
    assertThat(liveLogs.get(0).getStabilityBefore(), equalTo(0f));
    List<Snapshot> expected = nullOutMemoryStateColumns(liveLogs);

    RecallLogMemoryStateBackfill.Result result =
        RecallLogMemoryStateBackfill.run(recallLogRepository);

    assertThat(result.rowsBackfilled(), is(1));
    List<RecallLog> backfilled = orderedLogsFor(tracker);
    assertThat(Snapshot.of(backfilled.get(0)), equalTo(expected.get(0)));
  }

  @Test
  void leavesRowUnreplayableWhenStoredElapsedHoursDisagreesWithReplay() {
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).please();
    Timestamp t1 = makeMe.aTimestamp().of(1, 1).please();
    memoryTrackerService.markAsRecalled(t1, Grade.GOOD, tracker, null, null);
    Timestamp t2 = makeMe.aTimestamp().of(3, 1).please();
    memoryTrackerService.markAsRecalled(t2, Grade.GOOD, tracker, null, null);
    entityManager.flush();

    List<RecallLog> liveLogs = orderedLogsFor(tracker);
    RecallLog corrupted = liveLogs.get(1);
    corrupted.setElapsedHours(corrupted.getElapsedHours() + 999);
    recallLogRepository.saveAll(liveLogs);
    nullOutMemoryStateColumns(liveLogs);

    RecallLogMemoryStateBackfill.Result result =
        RecallLogMemoryStateBackfill.run(recallLogRepository);

    assertThat(result.rowsBackfilled(), is(1));
    List<RecallLog> backfilled = orderedLogsFor(tracker);
    assertThat(backfilled.get(0).getStabilityBefore(), notNullValue());
    assertThat(backfilled.get(1).getStabilityBefore(), nullValue());
    assertThat(backfilled.get(1).getDifficultyBefore(), nullValue());
    assertThat(backfilled.get(1).getRetrievability(), nullValue());
  }

  @Test
  void stopsAtTheLiveInstrumentedBoundaryAndLeavesItUntouched() {
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).please();
    Timestamp t1 = makeMe.aTimestamp().of(1, 1).please();
    memoryTrackerService.markAsRecalled(t1, Grade.GOOD, tracker, null, null);
    Timestamp t2 = makeMe.aTimestamp().of(3, 1).please();
    memoryTrackerService.markAsRecalled(t2, Grade.GOOD, tracker, null, null);
    entityManager.flush();

    entityManager.flush();
    entityManager.clear();
    List<RecallLog> liveLogs = orderedLogsFor(tracker);
    // Captured from a fresh read (post flush+clear) so it round-trips through the DB the same
    // way the post-backfill read below does — avoids comparing an in-memory value against one
    // that has been through JDBC.
    Snapshot liveExpected = Snapshot.of(liveLogs.get(1));

    // Only the older row predates instrumentation; the newer one stays "live".
    RecallLog older = liveLogs.get(0);
    older.setStabilityBefore(null);
    older.setDifficultyBefore(null);
    older.setRetrievability(null);
    recallLogRepository.save(older);
    entityManager.flush();
    entityManager.clear();

    RecallLogMemoryStateBackfill.Result result =
        RecallLogMemoryStateBackfill.run(recallLogRepository);

    assertThat(result.rowsBackfilled(), is(1));
    List<RecallLog> after = orderedLogsFor(tracker);
    assertThat(after.get(0).getStabilityBefore(), equalTo(0f));
    assertThat(Snapshot.of(after.get(1)), equalTo(liveExpected));
  }
}
