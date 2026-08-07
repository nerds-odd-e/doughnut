package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionType;
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

  private Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  private MemoryTracker dueTracker(Note note, Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerFor(note).nextRecallAt(nextRecallAt).please();
  }

  private MemoryTracker dueTracker(Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(nextRecallAt).please();
  }

  @Nested
  class Repeat {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.recalling("Asia/Shanghai", null));
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
      dueTracker(TimestampOperations.addHoursToTimestamp(currentTime, nextRecallAtHours));
      DueMemoryTrackers dueMemoryTrackers = controller.recalling(timezone, null);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(expectedCount));
    }

    @Test
    void shouldIncludePropertyKeyOnDueMemoryTrackerLite() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe.aMemoryTrackerFor(ownedNote()).propertyKey("topic").nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals("topic", dueMemoryTrackers.getToRepeat().get(0).getPropertyKey());
    }

    @Test
    void shouldIncludeRecallStatusInDueMemoryTrackers() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      dueTracker(currentTime);

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
    }

    @ParameterizedTest
    @CsvSource({
      "0,  12", "6,  6", "11, 1", "12, 12", "18, 6",
    })
    void shouldSetCurrentRecallWindowEndAtAlignedByHalfADay(
        int currentHour, int expectedHoursToAdd) {
      Timestamp currentTime = makeMe.aTimestamp().of(1, currentHour).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);
      dueTracker(currentTime);

      Timestamp expectedEndAt =
          TimestampOperations.addHoursToTimestamp(currentTime, expectedHoursToAdd);
      assertEquals(
          expectedEndAt, controller.recalling("Asia/Shanghai", 0).getCurrentRecallWindowEndAt());
      assertEquals(
          expectedEndAt, controller.recalling("Asia/Shanghai", 3).getCurrentRecallWindowEndAt());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note activeNote = ownedNote();
      Note deletedNote = ownedNote();
      dueTracker(activeNote, currentTime);
      dueTracker(deletedNote, currentTime);

      noteService.destroy(
          deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
    }

    @Test
    void shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note note = ownedNote();
      dueTracker(note, currentTime);
      makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
    }
  }

  @Nested
  class PreviouslyAnswered {
    @Test
    void shouldNotBeAbleToAccessWithoutLogin() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.previouslyAnswered("Asia/Shanghai"));
    }

    @Test
    void shouldReturnEmptyListWhenNoAnsweredRecallPrompts() {
      assertThat(controller.previouslyAnswered("Asia/Shanghai"), hasSize(0));
    }

    @Test
    void shouldReturnAnsweredRecallPromptsInCurrentWindow() {
      Timestamp currentTime = makeMe.aTimestamp().of(1, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);

      Note note = ownedNote();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(currentTime)
          .please();

      List<AnsweredQuestion> results = controller.previouslyAnswered("Asia/Shanghai");

      assertThat(results, hasSize(1));
      assertEquals(QuestionType.MCQ, results.get(0).getQuestionType());
    }

    @Test
    void shouldNotReturnAnsweredRecallPromptsFromPreviousWindow() {
      Timestamp previousWindowTime = makeMe.aTimestamp().of(0, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(previousWindowTime);

      Note note = ownedNote();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(previousWindowTime)
          .please();

      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 2).fromShanghai().please());

      assertThat(controller.previouslyAnswered("Asia/Shanghai"), hasSize(0));
    }

    @Test
    void shouldReturnSpellingResultsInCurrentWindow() {
      Timestamp currentTime = makeMe.aTimestamp().of(1, 2).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);

      Note note = ownedNote();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
      makeMe
          .aRecallPrompt()
          .forMemoryTracker(memoryTracker)
          .spelling()
          .answerSpelling("test answer")
          .answerTimestamp(currentTime)
          .please();

      assertEquals(
          QuestionType.SPELLING,
          controller.previouslyAnswered("Asia/Shanghai").get(0).getQuestionType());
    }
  }
}
