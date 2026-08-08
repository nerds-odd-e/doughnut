package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.controllers.dto.NoteRecallInfo;
import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionControllerTests extends LearningSessionControllerTestBase {

  @Autowired NoteController noteController;

  @Nested
  class Commission {
    @Test
    void commissionsSpanishNotebookWithDueCommissionedTrackers()
        throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
      Note gracias =
          makeMe.aNote().notebook(notebook).title("Gracias").content("Thank you").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayTwo).please();
      makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dayTwo).please();

      LearningSessionCommissionResponse response =
          controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      assertThat(response.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
      assertThat(response.getLearningSessionId(), greaterThan(0));

      String markdown = response.getRequestMarkdown();
      assertThat(markdown, containsString("# Learning Session Request"));
      assertThat(markdown, containsString("Notebook: Spanish conversation"));
      assertThat(
          markdown,
          containsString(
              "Teach the session items below, then return a Learning Session Report giving one"));
      assertThat(markdown, containsString("score from 0 to 5 per item"));
      assertThat(markdown, containsString("## Session Items"));
      assertThat(markdown, containsString("### Hola"));
      assertThat(markdown, containsString("### Gracias"));
      assertThat(markdown, containsString("Expected learning content: Hello"));
      assertThat(markdown, containsString("Expected learning content: Thank you"));
      assertThat(markdown, containsString("not yet tutored"));
      assertThat(markdown, containsString("- 5 — mastered the learning point with full fluency"));
      assertThat(markdown, containsString("- 4 — mastered the learning point with fluency"));
      assertThat(markdown, containsString("- 3 — mastered the learning point, but not fluent"));
      assertThat(
          markdown,
          containsString("- 2 — needed a reminder at first, then showed signs of mastering it"));
      assertThat(markdown, containsString("- 1 — needed several reminders"));
      assertThat(
          markdown, containsString("- 0 — could not reach the learning point even with help"));

      assertThat(
          sessionItemRepository.findByLearningSession_Id(response.getLearningSessionId()),
          hasSize(2));
      assertThat(learningSessionRepository.count(), equalTo(1L));
    }

    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = 1;
      assertThrows(
          ResponseStatusException.class, () -> controller.commission(request, "Asia/Shanghai"));
    }

    @Test
    void notebookNotFound() {
      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = 999_999;
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.commission(request, "Asia/Shanghai"));
      assertThat(ex.getStatusCode().value(), equalTo(404));
    }

    @Test
    void unauthorizedNotebook() {
      Notebook foreignNotebook = makeMe.aNotebook().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.commission(commissionRequest(foreignNotebook), "Asia/Shanghai"));
    }

    @Test
    void recommissionSameNotebookAbandonsPriorAwaitingReportSession()
        throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayTwo).please();

      LearningSessionCommissionResponse first =
          controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      Integer abandonedId = first.getLearningSessionId();

      LearningSessionCommissionResponse second =
          controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      assertThat(second.getLearningSessionId(), not(equalTo(abandonedId)));
      assertThat(
          learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
              currentUser.getUser().getId(),
              notebook.getId(),
              LearningSessionStatus.AWAITING_REPORT),
          hasSize(1));
      assertTrue(learningSessionRepository.findById(abandonedId).isEmpty());
      assertThat(learningSessionRepository.count(), equalTo(1L));
    }

    @Test
    void requestMarkdownReflectsPriorRecordedFeedbackPerTracker()
        throws UnexpectedNoAccessRightException {
      Timestamp priorSessionAt = makeMe.aTimestamp().of(5, 10).fromShanghai().please();
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).fromShanghai().please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
      Note gracias =
          makeMe.aNote().notebook(notebook).title("Gracias").content("Thank you").please();
      MemoryTracker holaTracker =
          makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayTwo).please();
      makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dayTwo).please();

      LearningSession recordedSession = recordedLearningSession(notebook, priorSessionAt);
      addRecordedFeedback(recordedSession, holaTracker, 4, priorSessionAt);

      LearningSessionCommissionResponse response =
          controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      String markdown = response.getRequestMarkdown();
      assertThat(markdown, containsString("### Hola"));
      assertThat(markdown, containsString("1 previous session, last on 1989-01-06"));
      assertThat(markdown, containsString("### Gracias"));
      assertThat(markdown, containsString("- Learning status: not yet tutored"));
    }

    @Test
    void recommissionPreservesRecordedSessionsForSameNotebook()
        throws UnexpectedNoAccessRightException {
      Timestamp priorSessionAt = makeMe.aTimestamp().of(5, 10).fromShanghai().please();
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).fromShanghai().please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
      MemoryTracker holaTracker =
          makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayTwo).please();

      LearningSession recordedSession = recordedLearningSession(notebook, priorSessionAt);
      addRecordedFeedback(recordedSession, holaTracker, 3, priorSessionAt);
      Integer recordedSessionId = recordedSession.getId();

      controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      assertTrue(learningSessionRepository.findById(recordedSessionId).isPresent());
      assertThat(
          learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
              currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED),
          hasSize(1));
    }

    @Test
    void emptyDueCommissionedTrackers() {
      Notebook notebook =
          makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Empty notebook").please();
      makeMe.aNote().notebook(notebook).title("Lonely").content("Alone").please();

      long sessionsBefore = learningSessionRepository.count();

      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.commission(commissionRequest(notebook), "Asia/Shanghai"));
      assertThat(ex.getStatusCode().value(), equalTo(400));
      assertThat(learningSessionRepository.count(), equalTo(sessionsBefore));
    }
  }

  @Nested
  class Record {
    private static final String HOLA_GRACIAS_REPORT =
        """
        # Learning Session Report

        Hola: 5
        Gracias: 1
        """;

    private static final String HOLA4_GRACIAS1_REPORT =
        """
        # Learning Session Report

        Hola: 4
        Gracias: 1
        """;

    @Test
    void recordsSpanishNotebookSessionWithMatchedScores() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      RecordLearningSessionResponse response =
          controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

      assertThat(response.getStatus(), equalTo(LearningSessionStatus.RECORDED));
      assertThat(response.getRecordedAt(), equalTo(dayTwo));
      assertThat(response.getRecordedItems(), hasSize(2));
      assertThat(response.getRejectedEntries(), empty());

      LearningSession session =
          learningSessionRepository
              .findByUser_IdAndNotebook_IdAndStatus(
                  currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
              .getFirst();
      assertThat(session.getRecordedAt(), equalTo(dayTwo));

      for (SessionItem item : sessionItemRepository.findByLearningSession_Id(session.getId())) {
        assertThat(item.getFeedbackScore(), notNullValue());
        assertThat(item.getFeedbackRecordedAt(), equalTo(dayTwo));
        assertThat(item.getMemoryTracker().getRecallCount(), equalTo(1));
      }
    }

    @Test
    void highScoreSchedulesLaterThanLowScoreFromSameStartingState()
        throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

      MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
      MemoryTracker graciasTracker = trackerForNote(notebook, "Gracias");

      assertThat(holaTracker.getNextRecallAt(), greaterThan(graciasTracker.getNextRecallAt()));
    }

    @Test
    void scoreZeroSchedulesStrictlyAfterRecordedAt() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      RecordLearningSessionResponse response =
          controller.record(
              recordRequest(notebook, "# Learning Session Report\n\nHola: 0\n"), "Asia/Shanghai");

      MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
      assertTrue(holaTracker.getNextRecallAt().after(response.getRecordedAt()));
    }

    @Test
    void allLinesRejectedStaysAwaitingReport() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");

      RecordLearningSessionResponse response =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  UnknownNote: 3
                  Hola: six
                  """),
              "Asia/Shanghai");

      assertThat(response.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
      assertThat(response.getRecordedItems(), empty());
      assertThat(response.getRejectedEntries(), hasSize(2));
      assertThat(
          learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
              currentUser.getUser().getId(),
              notebook.getId(),
              LearningSessionStatus.AWAITING_REPORT),
          hasSize(1));
    }

    @Test
    void notFoundWhenNoSessionToRecordOrAmend() {
      Notebook notebook =
          makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Lonely").please();

      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.record(recordRequest(notebook, "Hola: 5\n"), "Asia/Shanghai"));
      assertThat(ex.getStatusCode().value(), equalTo(404));
      assertThat(
          ex.getReason(), equalTo("No learning session to record or amend for this notebook."));
    }

    @Test
    void amendSpanishNotebookPartialReport() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      controller.record(recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

      LearningSession session =
          learningSessionRepository
              .findByUser_IdAndNotebook_IdAndStatus(
                  currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
              .getFirst();

      for (SessionItem item : sessionItemRepository.findByLearningSession_Id(session.getId())) {
        assertThat(item.getPreSessionRecallCount(), equalTo(0));
        assertThat(item.getPreSessionForgettingCurveIndex(), notNullValue());
      }

      Timestamp dayTwoLater = makeMe.aTimestamp().of(1, 10).please();
      testabilitySettings.timeTravelTo(dayTwoLater);

      RecordLearningSessionResponse amendResponse =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  Gracias: 4
                  """),
              "Asia/Shanghai");

      assertThat(amendResponse.getStatus(), equalTo(LearningSessionStatus.RECORDED));
      assertThat(amendResponse.getRecordedItems(), hasSize(1));
      assertThat(amendResponse.getRecordedItems().getFirst().getNoteTitle(), equalTo("Gracias"));
      assertThat(amendResponse.getRecordedItems().getFirst().getScore(), equalTo(4));

      SessionItem holaItem =
          sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Hola"))
              .findFirst()
              .orElseThrow();
      SessionItem graciasItem =
          sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Gracias"))
              .findFirst()
              .orElseThrow();

      assertThat(holaItem.getFeedbackScore(), equalTo(4));
      assertThat(graciasItem.getFeedbackScore(), equalTo(4));
      assertThat(holaItem.getMemoryTracker().getRecallCount(), equalTo(1));
      assertThat(graciasItem.getMemoryTracker().getRecallCount(), equalTo(1));

      Note graciasNote = graciasItem.getMemoryTracker().getNote();
      NoteRecallInfo noteInfo = noteController.getNoteInfo(graciasNote);
      MemoryTracker commissioned =
          noteInfo.getMemoryTrackers().stream()
              .filter(MemoryTracker::isCommissioned)
              .findFirst()
              .orElseThrow();
      assertThat(commissioned.getLatestTutorFeedbackScore(), equalTo(4));
    }

    @Test
    void partialAmendUpdatesOnlyMatchedItem() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      controller.record(recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

      LearningSession session =
          learningSessionRepository
              .findByUser_IdAndNotebook_IdAndStatus(
                  currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
              .getFirst();

      RecordLearningSessionResponse amendResponse =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  Hola: 5
                  """),
              "Asia/Shanghai");

      assertThat(amendResponse.getStatus(), equalTo(LearningSessionStatus.RECORDED));
      assertThat(amendResponse.getRecordedItems(), hasSize(1));
      assertThat(amendResponse.getRecordedItems().getFirst().getNoteTitle(), equalTo("Hola"));
      assertThat(amendResponse.getRecordedItems().getFirst().getScore(), equalTo(5));

      SessionItem holaItem =
          sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Hola"))
              .findFirst()
              .orElseThrow();
      SessionItem graciasItem =
          sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Gracias"))
              .findFirst()
              .orElseThrow();

      assertThat(holaItem.getFeedbackScore(), equalTo(5));
      assertThat(graciasItem.getFeedbackScore(), equalTo(1));
    }

    @Test
    void allRejectedAmendLeavesPriorFeedback() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      controller.record(recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

      LearningSession session =
          learningSessionRepository
              .findByUser_IdAndNotebook_IdAndStatus(
                  currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
              .getFirst();
      Timestamp originalRecordedAt = session.getRecordedAt();

      RecordLearningSessionResponse amendResponse =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  UnknownNote: 3
                  """),
              "Asia/Shanghai");

      assertThat(amendResponse.getStatus(), equalTo(LearningSessionStatus.RECORDED));
      assertThat(amendResponse.getRecordedAt(), equalTo(originalRecordedAt));
      assertThat(amendResponse.getRecordedItems(), empty());
      assertThat(amendResponse.getRejectedEntries(), hasSize(1));

      SessionItem holaItem =
          sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Hola"))
              .findFirst()
              .orElseThrow();
      SessionItem graciasItem =
          sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Gracias"))
              .findFirst()
              .orElseThrow();

      assertThat(holaItem.getFeedbackScore(), equalTo(4));
      assertThat(graciasItem.getFeedbackScore(), equalTo(1));
    }

    @Test
    void amendRejectsWhenPreSessionSnapshotMissing() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
      MemoryTracker holaTracker =
          makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayTwo).please();

      LearningSession recordedSession = recordedLearningSession(notebook, dayTwo);
      addRecordedFeedback(recordedSession, holaTracker, 4, dayTwo);

      SessionItem legacyItem =
          sessionItemRepository.findByLearningSession_Id(recordedSession.getId()).stream()
              .filter(item -> item.getNoteTitle().equals("Hola"))
              .findFirst()
              .orElseThrow();
      assertThat(legacyItem.getPreSessionRecallCount(), nullValue());

      RecordLearningSessionResponse amendResponse =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  Hola: 5
                  """,
                  recordedSession.getId()),
              "Asia/Shanghai");

      assertThat(amendResponse.getStatus(), equalTo(LearningSessionStatus.RECORDED));
      assertThat(amendResponse.getRecordedItems(), empty());
      assertThat(amendResponse.getRejectedEntries(), hasSize(1));
      assertThat(
          amendResponse.getRejectedEntries().getFirst().getReason(),
          equalTo("Cannot amend: no pre-session snapshot for this item."));
      assertThat(legacyItem.getFeedbackScore(), equalTo(4));
    }

    @Test
    void amendWithLearningSessionIdWhileAwaitingSessionExists()
        throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      controller.record(recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

      LearningSession recordedSession =
          learningSessionRepository
              .findByUser_IdAndNotebook_IdAndStatus(
                  currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
              .getFirst();
      Integer recordedSessionId = recordedSession.getId();

      Timestamp dayTwoLater = makeMe.aTimestamp().of(1, 10).please();
      testabilitySettings.timeTravelTo(dayTwoLater);

      LearningSession awaitingSession =
          makeMe
              .aLearningSession()
              .forNotebook(notebook)
              .by(currentUser.getUser())
              .status(LearningSessionStatus.AWAITING_REPORT)
              .commissionedAt(dayTwoLater)
              .please();
      for (SessionItem recordedItem :
          sessionItemRepository.findByLearningSession_Id(recordedSessionId)) {
        makeMe
            .aSessionItem()
            .learningSession(awaitingSession)
            .memoryTracker(recordedItem.getMemoryTracker())
            .please();
      }

      RecordLearningSessionResponse amendResponse =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  Gracias: 4
                  """,
                  recordedSessionId),
              "Asia/Shanghai");

      assertThat(amendResponse.getStatus(), equalTo(LearningSessionStatus.RECORDED));
      assertThat(amendResponse.getRecordedItems(), hasSize(1));
      assertThat(amendResponse.getRecordedItems().getFirst().getNoteTitle(), equalTo("Gracias"));
      assertThat(amendResponse.getRecordedItems().getFirst().getScore(), equalTo(4));

      LearningSession awaitingAfter =
          learningSessionRepository.findById(awaitingSession.getId()).orElseThrow();
      assertThat(awaitingAfter.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
      for (SessionItem item :
          sessionItemRepository.findByLearningSession_Id(awaitingSession.getId())) {
        assertThat(item.getFeedbackScore(), nullValue());
      }

      SessionItem recordedGraciasAfter =
          sessionItemRepository.findByLearningSession_Id(recordedSessionId).stream()
              .filter(item -> item.getNoteTitle().equals("Gracias"))
              .findFirst()
              .orElseThrow();
      assertThat(recordedGraciasAfter.getFeedbackScore(), equalTo(4));
    }

    @Test
    void highToLowAmendReschedulesFromSnapshot() throws UnexpectedNoAccessRightException {
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      testabilitySettings.timeTravelTo(dayTwo);

      Notebook notebook = spanishNotebook(dayTwo);
      controller.commission(commissionRequest(notebook), "Asia/Shanghai");
      controller.record(
          recordRequest(
              notebook,
              """
              # Learning Session Report

              Hola: 5
              Gracias: 1
              """),
          "Asia/Shanghai");

      MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
      Timestamp scheduleAfterScoreFive = holaTracker.getNextRecallAt();

      RecordLearningSessionResponse amendResponse =
          controller.record(
              recordRequest(
                  notebook,
                  """
                  # Learning Session Report

                  Hola: 1
                  """),
              "Asia/Shanghai");

      assertThat(amendResponse.getRecordedItems(), hasSize(1));
      assertThat(holaTracker.getNextRecallAt(), lessThan(scheduleAfterScoreFive));
    }

    private Notebook spanishNotebook(Timestamp dueAt) {
      Notebook notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .name("Spanish conversation")
              .please();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
      Note gracias =
          makeMe.aNote().notebook(notebook).title("Gracias").content("Thank you").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dueAt).please();
      makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dueAt).please();
      return notebook;
    }

    private MemoryTracker trackerForNote(Notebook notebook, String title) {
      LearningSession session =
          learningSessionRepository
              .findByUser_IdAndNotebook_IdAndStatus(
                  currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
              .getFirst();
      return sessionItemRepository.findByLearningSession_Id(session.getId()).stream()
          .filter(item -> item.getNoteTitle().equals(title))
          .map(SessionItem::getMemoryTracker)
          .findFirst()
          .orElseThrow();
    }
  }
}
