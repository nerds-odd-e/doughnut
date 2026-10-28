package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerTestIds;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.builders.MemoryTrackerBuilder;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptExcludeAsAmbiguousKeepsEarlierStateControllerTest
    extends RecallPromptControllerTestBase {
  @Autowired EntityManager entityManager;

  Note note;
  MemoryTracker tracker;
  RecallPrompt recallPrompt;
  Timestamp assimilatedAt;
  Timestamp recalledAt;

  @BeforeEach
  void setup() {
    assimilatedAt = makeMe.aTimestamp().of(0, 8).please();
    recalledAt = makeMe.aTimestamp().of(1, 20).please();
    note = ownedNote();
    tracker = aTrackerWithStateNoRecallLogRecords().please();
    recallPrompt = mcqPrompt(tracker, note);
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 8).please());
  }

  private MemoryTrackerBuilder aTrackerWithStateNoRecallLogRecords() {
    return makeMe
        .aMemoryTrackerFor(note)
        .assimilatedAt(assimilatedAt)
        .stabilityAndNextRecallAt(200.0f)
        .difficulty(6.0f);
  }

  @Test
  void theTrackerHoldsAgainTheStateItHadBeforeTheExcludedAttempt()
      throws UnexpectedNoAccessRightException {
    Timestamp lastRecalledAtBefore = tracker.getLastRecalledAt();
    Timestamp nextRecallAtBefore = tracker.getNextRecallAt();
    controller.answer(recallPrompt, choiceAnswer(1));

    controller.excludeAsAmbiguous(recallPrompt, ambiguousExclusion("both tenses fit"));

    assertThat(lastRecalledAtBefore, equalTo(assimilatedAt));
    assertTrackerHoldsTheFixtureStateDueAt(nextRecallAtBefore);
  }

  @Test
  void aLaterGradeIsAppliedToTheStateHeldBeforeTheExcludedAttempt()
      throws UnexpectedNoAccessRightException {
    answerWronglyThenRecallWell();

    controller.excludeAsAmbiguous(recallPrompt, ambiguousExclusion("both tenses fit"));

    assertPersistedStateIsGoodRecallOn(aTrackerWithStateNoRecallLogRecords().inMemoryPlease());
  }

  @Test
  void anExcludedAttemptWithoutCachedStateRebuildsFromANewTracker()
      throws UnexpectedNoAccessRightException {
    answerWronglyThenRecallWell();
    RecallLog excludedLog = persistedLogs(tracker).get(0);
    entityManager
        .createNativeQuery(
            "UPDATE recall_log SET stability_before = NULL, difficulty_before = NULL,"
                + " retrievability = NULL WHERE id = :id")
        .setParameter("id", excludedLog.getId())
        .executeUpdate();
    makeMe.refresh(excludedLog);

    controller.excludeAsAmbiguous(recallPrompt, ambiguousExclusion("both tenses fit"));

    assertPersistedStateIsGoodRecallOn(
        makeMe.aMemoryTrackerFor(note).assimilatedAt(assimilatedAt).inMemoryPlease());
  }

  @Test
  void aLaterGradeCountsItsElapsedTimeFromTheLoggedGradeBeforeTheExcludedAttempt()
      throws UnexpectedNoAccessRightException {
    Timestamp firstRecalledAt = makeMe.aTimestamp().of(1, 8).please();
    memoryTrackerController.markAsRecalled(tracker, Grade.GOOD);
    MemoryTracker gradedBefore = aTrackerWithStateNoRecallLogRecords().inMemoryPlease();
    gradedBefore.applyGrade(firstRecalledAt, Grade.GOOD);
    testabilitySettings.timeTravelTo(minutesAfter(makeMe.aTimestamp().of(2, 8).please(), 30));
    recalledAt = minutesAfter(makeMe.aTimestamp().of(3, 8).please(), 15);
    answerWronglyThenRecallWell();

    controller.excludeAsAmbiguous(recallPrompt, ambiguousExclusion("both tenses fit"));

    assertPersistedStateIsGoodRecallOn(gradedBefore);
    assertThat(persistedLogs(tracker).getLast().getElapsedHours(), equalTo(48));
  }

  @Test
  void anAttemptWithoutARecallLogIsExcludedAndChangesNoSchedule()
      throws UnexpectedNoAccessRightException {
    RecallPrompt answeredWithoutLog =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(tracker)
            .withMcqForNote(note)
            .answerChoiceIndex(1)
            .please();
    Timestamp nextRecallAtBefore = tracker.getNextRecallAt();

    controller.excludeAsAmbiguous(answeredWithoutLog, ambiguousExclusion("both tenses fit"));

    assertThat(makeMe.refresh(answeredWithoutLog.getAnswer()).getExcludedAt(), notNullValue());
    assertThat(persistedLogs(tracker), empty());
    assertTrackerHoldsTheFixtureStateDueAt(nextRecallAtBefore);
  }

  @Test
  void excludingTwoAttemptsLaterOneFirstLeavesTheStateHeldBeforeBoth()
      throws UnexpectedNoAccessRightException {
    Timestamp nextRecallAtBefore = tracker.getNextRecallAt();
    RecallPrompt laterPrompt = answerWronglyTwice();

    excludeInOrder(laterPrompt, recallPrompt);

    assertTrackerHoldsTheFixtureStateDueAt(nextRecallAtBefore);
  }

  @Test
  void excludingTwoAttemptsEarlierOneFirstLeavesTheStateHeldBeforeBoth()
      throws UnexpectedNoAccessRightException {
    Timestamp nextRecallAtBefore = tracker.getNextRecallAt();
    RecallPrompt laterPrompt = answerWronglyTwice();

    excludeInOrder(recallPrompt, laterPrompt);

    assertTrackerHoldsTheFixtureStateDueAt(nextRecallAtBefore);
  }

  @Test
  void confirmingAnExcludedAttemptAgainLeavesTheScheduleAsItIs()
      throws UnexpectedNoAccessRightException {
    RecallPrompt laterPrompt = answerWronglyTwice();
    excludeInOrder(laterPrompt, recallPrompt);
    List<Object> scheduleBefore = persistedSchedule();

    controller.excludeAsAmbiguous(laterPrompt, ambiguousExclusion("asked again"));

    assertThat(persistedSchedule(), equalTo(scheduleBefore));
  }

  /** Answers {@code recallPrompt} wrongly, then a later prompt on the same tracker, returned. */
  private RecallPrompt answerWronglyTwice() throws UnexpectedNoAccessRightException {
    controller.answer(recallPrompt, choiceAnswer(1));
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 8).please());
    RecallPrompt laterPrompt = mcqPrompt(tracker, note);
    controller.answer(laterPrompt, choiceAnswer(1));
    return laterPrompt;
  }

  private void excludeInOrder(RecallPrompt first, RecallPrompt second)
      throws UnexpectedNoAccessRightException {
    controller.excludeAsAmbiguous(first, ambiguousExclusion("both tenses fit"));
    controller.excludeAsAmbiguous(second, ambiguousExclusion("both tenses fit"));
  }

  private void assertTrackerHoldsTheFixtureStateDueAt(Timestamp nextRecallAt) {
    makeMe.refresh(tracker);
    assertThat((double) tracker.getStability(), closeTo(200.0, 1e-4));
    assertThat((double) tracker.getDifficulty(), closeTo(6.0, 1e-4));
    assertThat(tracker.getLastRecalledAt(), equalTo(assimilatedAt));
    assertThat(tracker.getNextRecallAt(), equalTo(nextRecallAt));
  }

  /** The tracker's scheduling state and each log's cached state, as the database holds them. */
  private List<Object> persistedSchedule() {
    List<RecallLog> logs = persistedLogs(tracker);
    makeMe.refresh(tracker);
    List<Object> schedule =
        new ArrayList<>(
            Arrays.asList(
                tracker.getStability(),
                tracker.getDifficulty(),
                tracker.getLastRecalledAt(),
                tracker.getNextRecallAt()));
    logs.forEach(
        log ->
            schedule.addAll(
                Arrays.asList(
                    log.getId(),
                    log.getStabilityBefore(),
                    log.getDifficultyBefore(),
                    log.getRetrievability(),
                    log.getElapsedHours())));
    return schedule;
  }

  private Timestamp minutesAfter(Timestamp timestamp, int minutes) {
    return Timestamp.from(timestamp.toInstant().plusSeconds(minutes * 60L));
  }

  private void answerWronglyThenRecallWell() throws UnexpectedNoAccessRightException {
    controller.answer(recallPrompt, choiceAnswer(1));
    testabilitySettings.timeTravelTo(recalledAt);
    memoryTrackerController.markAsRecalled(tracker, Grade.GOOD);
  }

  /** {@code start} is an in-memory tracker in the state the replay is expected to start from. */
  private void assertPersistedStateIsGoodRecallOn(MemoryTracker start) {
    MemoryTrackerTestIds.assign(start, tracker.getId());
    RecallLog onlyLog =
        makeMe.aRecallLogFor(start).recordedAt(recalledAt).grade(Grade.GOOD).inMemoryPlease();
    onlyLog.recordStateBefore(start);
    start.applyGrade(recalledAt, Grade.GOOD);

    makeMe.refresh(tracker);
    assertAbout(tracker.getStability(), start.getStability());
    assertAbout(tracker.getDifficulty(), start.getDifficulty());
    assertThat(tracker.getLastRecalledAt(), equalTo(recalledAt));
    assertThat(tracker.getNextRecallAt(), equalTo(start.getNextRecallAt()));
    RecallLog goodLog = persistedLogs(tracker).getLast();
    assertThat(goodLog.getGrade(), equalTo(Grade.GOOD));
    assertThat(goodLog.getElapsedHours(), equalTo(onlyLog.getElapsedHours()));
    assertAbout(goodLog.getStabilityBefore(), onlyLog.getStabilityBefore());
    assertAbout(goodLog.getDifficultyBefore(), onlyLog.getDifficultyBefore());
    assertAbout(goodLog.getRetrievability(), onlyLog.getRetrievability());
  }

  /** MySQL returns a FLOAT column with about six significant digits. */
  private void assertAbout(Number persisted, Number expected) {
    if (expected == null) {
      assertThat(persisted, nullValue());
      return;
    }
    double value = expected.doubleValue();
    assertThat(persisted.doubleValue(), closeTo(value, Math.max(1.0, Math.abs(value)) * 1e-5));
  }
}
