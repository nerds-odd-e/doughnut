package com.odde.donut.entities;

import static com.odde.donut.entities.Fsrs.NEW_STABILITY_HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryTrackerRebuildFromRecallHistoryTest extends MemoryTrackerRecallSchedulingTestBase {
  record StateBefore(int elapsedHours, Float stability, Float difficulty, Double retrievability) {
    static StateBefore cachedOn(RecallLog log) {
      return new StateBefore(
          log.getElapsedHours(),
          log.getStabilityBefore(),
          log.getDifficultyBefore(),
          log.getRetrievability());
    }
  }

  final Timestamp assimilatedAt = makeMe.aTimestamp().of(1, 8).please();
  final MemoryTracker incremental = aNewTracker();
  final List<RecallLog> incrementalLogs = new ArrayList<>();

  MemoryTracker aNewTracker() {
    return makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(assimilatedAt).inMemoryPlease();
  }

  RecallLog anExcludedAttemptWithoutCachedState(MemoryTracker tracker, int hoursAfterAssimilation) {
    Answer answer = new Answer();
    answer.excludeAsAmbiguous("both tenses fit", hoursAfterAssimilation(hoursAfterAssimilation));
    return makeMe
        .aRecallLogFor(tracker)
        .recordedAt(hoursAfterAssimilation(hoursAfterAssimilation))
        .grade(Grade.AGAIN)
        .answer(answer)
        .inMemoryPlease();
  }

  Timestamp hoursAfterAssimilation(int hours) {
    return TimestampOperations.addHoursToTimestamp(assimilatedAt, hours);
  }

  /** {@code grade} null is Confusion, as in {@code MemoryTrackerService.persistRecallLog}. */
  void recordIncrementally(int hoursAfterAssimilation, Grade grade) {
    Timestamp recordedAt = hoursAfterAssimilation(hoursAfterAssimilation);
    RecallLog log =
        makeMe.aRecallLogFor(incremental).recordedAt(recordedAt).grade(grade).inMemoryPlease();
    log.recordStateBefore(incremental);
    incrementalLogs.add(log);
    if (grade == null) {
      incremental.adjustForConfusion(recordedAt);
    } else {
      incremental.applyGrade(recordedAt, grade);
    }
  }

  @Test
  void rebuildingWithoutAnAttemptThatHoldsNoCachedStateReproducesIncrementalScheduling() {
    recordIncrementally(5, Grade.AGAIN);
    recordIncrementally(30, Grade.GOOD);
    recordIncrementally(40, null);
    recordIncrementally(130, Grade.HARD);
    MemoryTracker rebuilt = aNewTracker();
    List<RecallLog> replayedLogs =
        incrementalLogs.stream()
            .map(
                log ->
                    makeMe
                        .aRecallLogFor(rebuilt)
                        .recordedAt(log.getRecordedAt())
                        .grade(log.getGrade())
                        .inMemoryPlease())
            .toList();
    RecallLog excluded = anExcludedAttemptWithoutCachedState(rebuilt, 20);
    List<RecallLog> history = new ArrayList<>(replayedLogs);
    history.add(1, excluded);

    rebuilt.rebuildWithout(excluded, history);

    assertThat(rebuilt.getStability(), equalTo(incremental.getStability()));
    assertThat(rebuilt.getDifficulty(), equalTo(incremental.getDifficulty()));
    assertThat(rebuilt.getLastRecalledAt(), equalTo(incremental.getLastRecalledAt()));
    assertThat(rebuilt.getNextRecallAt(), equalTo(incremental.getNextRecallAt()));
    assertThat(
        replayedLogs.stream().map(StateBefore::cachedOn).toList(),
        equalTo(incrementalLogs.stream().map(StateBefore::cachedOn).toList()));
    assertThat(
        replayedLogs.stream().map(RecallLog::getElapsedHours).toList(), contains(0, 25, 10, 100));
  }

  @Test
  void aTrackerLeftWithNoValidHistoryIsNewAndDueAtAssimilation() {
    MemoryTracker tracker = aNewTracker();
    RecallLog excluded = anExcludedAttemptWithoutCachedState(tracker, 5);
    tracker.applyGrade(excluded.getRecordedAt(), excluded.getGrade());

    tracker.rebuildWithout(excluded, List.of(excluded));

    assertThat(tracker.getStability(), equalTo(NEW_STABILITY_HOURS));
    assertThat(tracker.getDifficulty(), nullValue());
    assertThat(tracker.getLastRecalledAt(), nullValue());
    assertThat(tracker.getNextRecallAt(), equalTo(assimilatedAt));
  }
}
