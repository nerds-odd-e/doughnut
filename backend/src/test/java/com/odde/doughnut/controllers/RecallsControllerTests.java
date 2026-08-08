package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
  @Autowired LearningSessionController learningSessionController;
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

  private RecordLearningSessionRequest recordRequest(Notebook notebook, String reportMarkdown) {
    RecordLearningSessionRequest request = new RecordLearningSessionRequest();
    request.notebookId = notebook.getId();
    request.reportMarkdown = reportMarkdown;
    return request;
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

    @Test
    void shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note note =
          makeMe
              .aNote()
              .notebook(
                  makeMe
                      .aNotebook()
                      .creatorAndOwner(currentUser.getUser())
                      .name("Spanish conversation")
                      .please())
              .please();
      MemoryTracker commissioned =
          makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(0));
      assertThat(dueMemoryTrackers.getDueCommissioned(), hasSize(1));
      assertEquals(
          commissioned.getId(), dueMemoryTrackers.getDueCommissioned().get(0).getMemoryTrackerId());
      assertEquals(
          note.getNotebook().getId(),
          dueMemoryTrackers.getDueCommissioned().get(0).getNotebookId());
      assertEquals(
          "Spanish conversation", dueMemoryTrackers.getDueCommissioned().get(0).getNotebookName());
    }

    @Test
    void excludesDueCommissionedTrackersAwaitingReportAfterCommission()
        throws UnexpectedNoAccessRightException {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(currentTime).please();

      assertThat(controller.recalling("Asia/Shanghai", 0).getDueCommissioned(), hasSize(1));

      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = notebook.getId();
      learningSessionController.commission(request, "Asia/Shanghai");

      DueMemoryTrackers afterCommission = controller.recalling("Asia/Shanghai", 0);
      assertThat(afterCommission.getDueCommissioned(), hasSize(0));
      assertThat(afterCommission.getToRepeat(), hasSize(0));
    }

    @Test
    void returnsAwaitingReportSessionsAfterCommission() throws UnexpectedNoAccessRightException {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(currentTime).please();

      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = notebook.getId();
      learningSessionController.commission(request, "Asia/Shanghai");

      DueMemoryTrackers afterCommission = controller.recalling("Asia/Shanghai", 0);
      assertThat(afterCommission.getAwaitingReportSessions(), hasSize(1));
      assertEquals(
          "Spanish conversation",
          afterCommission.getAwaitingReportSessions().get(0).getNotebookName());
      assertEquals(
          notebook.getId(), afterCommission.getAwaitingReportSessions().get(0).getNotebookId());
      assertThat(
          afterCommission.getAwaitingReportSessions().get(0).getRequestMarkdown(),
          org.hamcrest.Matchers.containsString("### Hola"));
      assertThat(afterCommission.getDueCommissioned(), hasSize(0));
    }

    @Test
    void dayThreeDueCommissionedOnlyGraciasAfterRecordedScores()
        throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").please());
      Timestamp dayOne = makeMe.aTimestamp().of(0, 8).please();
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      Timestamp dayThree = makeMe.aTimestamp().of(2, 9).please();
      testabilitySettings.timeTravelTo(dayOne);

      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      Note gracias = makeMe.aNote().notebook(notebook).title("Gracias").please();
      MemoryTracker holaTracker =
          makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayOne).please();
      MemoryTracker graciasTracker =
          makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dayOne).please();

      testabilitySettings.timeTravelTo(dayTwo);
      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = notebook.getId();
      learningSessionController.commission(request, "Asia/Shanghai");
      learningSessionController.record(
          recordRequest(
              notebook,
              """
              # Learning Session Report

              Hola: 5
              Gracias: 1
              """),
          "Asia/Shanghai");

      testabilitySettings.timeTravelTo(dayThree);
      DueMemoryTrackers due = controller.recalling("Asia/Shanghai", 0);

      assertThat(due.getDueCommissioned(), hasSize(1));
      assertEquals(graciasTracker.getId(), due.getDueCommissioned().get(0).getMemoryTrackerId());
      assertTrue(holaTracker.getNextRecallAt().after(dayThree));
    }

    @Test
    void awaitingReportExclusionDoesNotLeakAcrossUsers() throws UnexpectedNoAccessRightException {
      User userA = currentUser.getUser();
      User userB = makeMe.aUser().please();
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);

      Notebook notebookA =
          makeMe.aNotebook().creatorAndOwner(userA).name("Spanish conversation").please();
      Note holaA = makeMe.aNote().notebook(notebookA).title("Hola").please();
      makeMe.aMemoryTrackerFor(holaA).commissioned().nextRecallAt(currentTime).please();

      Notebook notebookB = makeMe.aNotebook().creatorAndOwner(userB).name("Kanji").please();
      Note noteB = makeMe.aNote().notebook(notebookB).title("水").please();
      MemoryTracker trackerB =
          makeMe.aMemoryTrackerFor(noteB).commissioned().nextRecallAt(currentTime).please();

      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = notebookA.getId();
      learningSessionController.commission(request, "Asia/Shanghai");

      currentUser.setUser(userB);
      DueMemoryTrackers dueForB = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueForB.getDueCommissioned(), hasSize(1));
      assertEquals(trackerB.getId(), dueForB.getDueCommissioned().get(0).getMemoryTrackerId());
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
