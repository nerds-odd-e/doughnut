package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecallPromptRepositoryRecentRecallUsersTest {
  @Autowired MakeMe makeMe;
  @Autowired RecallPromptRepository recallPromptRepository;

  User user;
  Note note;
  MemoryTracker memoryTracker;
  Timestamp windowStart;
  Timestamp windowEnd;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    windowEnd = makeMe.aTimestamp().of(15, 8).fromShanghai().please();
    windowStart = new Timestamp(windowEnd.getTime() - TimeUnit.DAYS.toMillis(7));
  }

  @Test
  void returnsEmptyWhenNoAnsweredRecallsExist() {
    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, empty());
  }

  @Test
  void returnsUserWhenOneRecallAnsweredInWindow() {
    Timestamp oneHourBeforeEnd = new Timestamp(windowEnd.getTime() - TimeUnit.HOURS.toMillis(1));
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(oneHourBeforeEnd)
        .please();

    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, containsInAnyOrder(user.getId()));
  }

  @Test
  void excludesRecallAnsweredBeforeWindowStart() {
    Timestamp beforeWindow = new Timestamp(windowStart.getTime() - TimeUnit.MINUTES.toMillis(1));
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(beforeWindow)
        .please();

    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, empty());
  }

  @Test
  void includesRecallAnsweredAtWindowStart() {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(windowStart)
        .please();

    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, containsInAnyOrder(user.getId()));
  }

  @Test
  void excludesRecallAnsweredAtWindowEnd() {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(windowEnd)
        .please();

    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, empty());
  }

  @Test
  void returnsDistinctUserIdsWhenMultipleRecallsInWindow() {
    Timestamp oneHourBeforeEnd = new Timestamp(windowEnd.getTime() - TimeUnit.HOURS.toMillis(1));
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(oneHourBeforeEnd)
        .please();
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(new Timestamp(windowEnd.getTime() - TimeUnit.HOURS.toMillis(2)))
        .please();

    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, containsInAnyOrder(user.getId()));
  }

  @Test
  void returnsMultipleUsersWithRecallsInWindow() {
    Timestamp oneHourBeforeEnd = new Timestamp(windowEnd.getTime() - TimeUnit.HOURS.toMillis(1));
    User otherUser = makeMe.aUser().please();
    Note otherNote = makeMe.aNote().notebookOwnedBy(otherUser).please();
    MemoryTracker otherTracker = makeMe.aMemoryTrackerFor(otherNote).by(otherUser).please();

    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(oneHourBeforeEnd)
        .please();
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(otherNote)
        .forMemoryTracker(otherTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(oneHourBeforeEnd)
        .please();

    List<Integer> userIds =
        recallPromptRepository.findUserIdsWithAnsweredRecallsInTimeRange(windowStart, windowEnd);

    assertThat(userIds, containsInAnyOrder(user.getId(), otherUser.getId()));
  }
}
