package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.AmbiguousExclusionDTO;
import com.odde.donut.entities.Answer;
import com.odde.donut.entities.Fsrs;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.exceptions.QuestionAnswerException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.UserService;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptExcludeAsAmbiguousControllerTest extends RecallPromptControllerTestBase {
  @Autowired RecallLogRepository recallLogRepository;
  @Autowired UserService userService;

  Note note;
  MemoryTracker tracker;
  RecallPrompt recallPrompt;
  Timestamp assimilatedAt;
  Timestamp answeredAt;
  Timestamp excludedAt;

  @BeforeEach
  void setup() throws UnexpectedNoAccessRightException {
    assimilatedAt = makeMe.aTimestamp().of(0, 8).please();
    answeredAt = makeMe.aTimestamp().of(1, 8).please();
    excludedAt = makeMe.aTimestamp().of(2, 8).please();
    note = ownedNote();
    tracker = makeMe.aMemoryTrackerFor(note).assimilatedAt(assimilatedAt).please();
    recallPrompt = mcqPrompt(tracker, note);
    testabilitySettings.timeTravelTo(answeredAt);
    controller.answer(recallPrompt, choiceAnswer(1));
    testabilitySettings.timeTravelTo(excludedAt);
  }

  @Test
  void recordsTheExclusionAndKeepsTheOriginalMark() throws UnexpectedNoAccessRightException {
    Answer answer =
        controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit")).getAnswer();

    makeMe.refresh(answer);
    assertThat(answer.getExcludedAt(), equalTo(excludedAt));
    assertThat(answer.getExclusionReason(), equalTo("both tenses fit"));
    assertThat(answer.getCorrect(), is(false));
  }

  @Test
  void repeatingReturnsTheFirstDecision() throws UnexpectedNoAccessRightException {
    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(3, 8).please());

    Answer answer =
        controller.excludeAsAmbiguous(recallPrompt, exclusion("another reason")).getAnswer();

    assertThat(answer.getExcludedAt(), equalTo(excludedAt));
    assertThat(answer.getExclusionReason(), equalTo("both tenses fit"));
  }

  @Test
  void leavesAnotherLearnersAttemptOnTheSameQuestionUntouched()
      throws UnexpectedNoAccessRightException {
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(memoryTrackerOwnedByAnotherUser())
            .withMcq(recallPrompt.getMcq())
            .answerChoiceIndex(1)
            .please();

    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));

    assertThat(makeMe.refresh(othersPrompt.getAnswer()).getExcludedAt(), nullValue());
  }

  @Test
  void refusesAnotherLearnersAttempt() {
    MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(othersTracker)
            .withMcqForNote(othersTracker.getNote())
            .answerChoiceIndex(1)
            .please();

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.excludeAsAmbiguous(othersPrompt, exclusion("both tenses fit")));
  }

  @Test
  void refusesAnUnansweredPrompt() {
    RecallPrompt unanswered = mcqPrompt(tracker, note);

    assertThrows(
        QuestionAnswerException.class,
        () -> controller.excludeAsAmbiguous(unanswered, exclusion("both tenses fit")));
  }

  @Test
  void anAttemptWithNoOtherHistoryLeavesTheSameTrackerNewAndDueAtAssimilation()
      throws UnexpectedNoAccessRightException {
    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));

    makeMe.refresh(tracker);
    assertThat(tracker.getStability(), equalTo(Fsrs.NEW_STABILITY_HOURS));
    assertThat(tracker.getDifficulty(), nullValue());
    assertThat(tracker.getLastRecalledAt(), nullValue());
    assertThat(tracker.getNextRecallAt(), equalTo(assimilatedAt));
    assertThat(
        userService.getMemoryTrackersFor(currentUser.getUser(), note).stream()
            .map(MemoryTracker::getId)
            .toList(),
        contains(tracker.getId()));
    assertThat(persistedLogs().stream().map(RecallLog::getGrade).toList(), contains(Grade.AGAIN));
  }

  @Test
  void aLaterGradeIsRescheduledAsIfTheExcludedAttemptHadNotHappened()
      throws UnexpectedNoAccessRightException {
    Timestamp recalledAt = makeMe.aTimestamp().of(1, 20).please();
    testabilitySettings.timeTravelTo(recalledAt);
    memoryTrackerController.markAsRecalled(tracker, Grade.GOOD);
    List<Object> excludedLogBefore = cachedOn(persistedLogs().get(0));
    testabilitySettings.timeTravelTo(excludedAt);
    MemoryTracker gradedOnce =
        makeMe.aMemoryTrackerFor(note).assimilatedAt(assimilatedAt).inMemoryPlease();
    RecallLog onlyLog =
        makeMe.aRecallLogFor(gradedOnce).recordedAt(recalledAt).grade(Grade.GOOD).inMemoryPlease();
    onlyLog.recordStateBefore(gradedOnce);
    gradedOnce.applyGrade(recalledAt, Grade.GOOD);

    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));

    makeMe.refresh(tracker);
    assertThat((double) tracker.getStability(), closeTo(gradedOnce.getStability(), 1e-4));
    assertThat((double) tracker.getDifficulty(), closeTo(gradedOnce.getDifficulty(), 1e-4));
    assertThat(tracker.getLastRecalledAt(), equalTo(gradedOnce.getLastRecalledAt()));
    assertThat(tracker.getNextRecallAt(), equalTo(gradedOnce.getNextRecallAt()));
    List<RecallLog> logs = persistedLogs();
    assertThat(logs.stream().map(RecallLog::getGrade).toList(), contains(Grade.AGAIN, Grade.GOOD));
    assertThat(cachedOn(logs.get(0)), equalTo(excludedLogBefore));
    assertThat(cachedOn(logs.get(1)), equalTo(cachedOn(onlyLog)));
  }

  @Test
  void repeatingChangesNeitherTheScheduleNorTheHistory() throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 20).please());
    memoryTrackerController.markAsRecalled(tracker, Grade.GOOD);
    controller.excludeAsAmbiguous(recallPrompt, exclusion("both tenses fit"));
    List<Object> afterFirst = persistedScheduleAndHistory();
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(3, 8).please());

    controller.excludeAsAmbiguous(recallPrompt, exclusion("another reason"));

    assertThat(persistedScheduleAndHistory(), equalTo(afterFirst));
  }

  private List<RecallLog> persistedLogs() {
    makeMe.entityPersister.flush();
    List<RecallLog> logs =
        recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtAscIdAsc(tracker.getId());
    logs.forEach(makeMe::refresh);
    return logs;
  }

  private List<Object> cachedOn(RecallLog log) {
    return Arrays.asList(
        log.getElapsedHours(),
        log.getStabilityBefore(),
        log.getDifficultyBefore(),
        log.getRetrievability());
  }

  private List<Object> persistedScheduleAndHistory() {
    makeMe.refresh(tracker);
    return Arrays.asList(
        tracker.getStability(),
        tracker.getDifficulty(),
        tracker.getLastRecalledAt(),
        tracker.getNextRecallAt(),
        persistedLogs().stream().map(log -> List.of(log.getId(), cachedOn(log))).toList());
  }

  private AmbiguousExclusionDTO exclusion(String reason) {
    AmbiguousExclusionDTO dto = new AmbiguousExclusionDTO();
    dto.setReason(reason);
    return dto;
  }
}
