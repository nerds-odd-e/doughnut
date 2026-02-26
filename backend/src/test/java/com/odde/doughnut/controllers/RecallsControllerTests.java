package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallResult;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class RecallsControllerTests extends ControllerTestBase {
  @Autowired RecallsController controller;
  @Autowired NoteService noteService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  RecallsController nullUserController() {
    currentUser.setUser(null);
    return controller;
  }

  @Nested
  class Repeat {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().recalling("Asia/Shanghai", null));
    }

    @ParameterizedTest
    @CsvSource(
        useHeadersInDisplayName = true,
        delimiter = '|',
        textBlock =
            """
                next recall at (in hours) | timezone     | expected count
                #------------------------------------------------------------
                -1                        | Asia/Tokyo   | 1
                0                         | Asia/Tokyo   | 1
                4                         | Asia/Tokyo   | 0
                4                         | Europe/Paris | 1
                12                        | Europe/Paris | 0
                """)
    void shouldGetMemoryTrackersBasedOnTimezone(
        int nextRecallAtHours, String timezone, int expectedCount) {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe
          .aMemoryTrackerBy(currentUser.getUser())
          .nextRecallAt(TimestampOperations.addHoursToTimestamp(currentTime, nextRecallAtHours))
          .please();
      DueMemoryTrackers dueMemoryTrackers = controller.recalling(timezone, null);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(expectedCount));
    }

    @Test
    void shouldIncludeRecallStatusInDueMemoryTrackers() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals(1, dueMemoryTrackers.getToRepeat().size());
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
      // currentTime is 1989-01-01 00:00:00 UTC, which is 1989-01-01 08:00:00 in Asia/Shanghai
      // Since hour < 12, alignByHalfADay returns same day at 12:00:00 Asia/Shanghai = 04:00:00 UTC
      Timestamp expectedEndAt = TimestampOperations.addHoursToTimestamp(currentTime, 4);
      assertEquals(expectedEndAt, dueMemoryTrackers.getCurrentRecallWindowEndAt());
    }

    @ParameterizedTest
    @CsvSource({
      "0,  12", "6,  6", "11, 1", "12, 12", "18, 6",
    })
    void shouldSetCurrentRecallWindowEndAtAlignedByHalfADay(
        int currentHour, int expectedHoursToAdd) {
      Timestamp currentTime = makeMe.aTimestamp().of(1, currentHour).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      Timestamp expectedEndAt =
          TimestampOperations.addHoursToTimestamp(currentTime, expectedHoursToAdd);
      assertEquals(expectedEndAt, dueMemoryTrackers.getCurrentRecallWindowEndAt());
      // Verify it's not affected by dueInDays
      DueMemoryTrackers dueMemoryTrackersWithDays = controller.recalling("Asia/Shanghai", 3);
      assertEquals(expectedEndAt, dueMemoryTrackersWithDays.getCurrentRecallWindowEndAt());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note activeNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note deletedNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      makeMe
          .aMemoryTrackerFor(activeNote)
          .by(currentUser.getUser())
          .nextRecallAt(currentTime)
          .please();
      makeMe
          .aMemoryTrackerFor(deletedNote)
          .by(currentUser.getUser())
          .nextRecallAt(currentTime)
          .please();

      noteService.destroy(deletedNote);

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals(1, dueMemoryTrackers.getToRepeat().size());
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
    }
  }

  @Nested
  class PreviouslyAnswered {
    @Test
    void shouldNotBeAbleToAccessWithoutLogin() {
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().previouslyAnswered("Asia/Shanghai"));
    }

    @Test
    void shouldReturnEmptyListWhenNoAnsweredRecallPrompts() {
      List<RecallResult> results = controller.previouslyAnswered("Asia/Shanghai");
      assertThat(results, hasSize(0));
    }

    @Test
    void shouldReturnAnsweredRecallPromptsInCurrentWindow() {
      // Set current time to 10:00 AM Shanghai time (window: 0:00-12:00)
      Timestamp currentTime = makeMe.aTimestamp().of(1, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);

      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      // Create an answered recall prompt with answer timestamp within current window
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(currentTime)
          .please();

      List<RecallResult> results = controller.previouslyAnswered("Asia/Shanghai");

      assertThat(results, hasSize(1));
      assertThat(results.get(0), instanceOf(RecallResult.QuestionResult.class));
    }

    @Test
    void shouldNotReturnAnsweredRecallPromptsFromPreviousWindow() {
      // Answer a question in a previous window (yesterday at 10:00 AM)
      Timestamp previousWindowTime = makeMe.aTimestamp().of(0, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(previousWindowTime);

      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(previousWindowTime)
          .please();

      // Move to current window (next day at 10:00 AM)
      Timestamp currentTime = makeMe.aTimestamp().of(1, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);

      List<RecallResult> results = controller.previouslyAnswered("Asia/Shanghai");

      assertThat(results, hasSize(0));
    }

    @Test
    void shouldReturnSpellingResultsInCurrentWindow() {
      Timestamp currentTime = makeMe.aTimestamp().of(1, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);

      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      // Create an answered spelling recall prompt
      makeMe
          .aRecallPrompt()
          .forMemoryTracker(memoryTracker)
          .spelling()
          .answerSpelling("test answer")
          .answerTimestamp(currentTime)
          .please();

      List<RecallResult> results = controller.previouslyAnswered("Asia/Shanghai");

      assertThat(results, hasSize(1));
      assertThat(results.get(0), instanceOf(RecallResult.SpellingResult.class));
    }
  }
}
