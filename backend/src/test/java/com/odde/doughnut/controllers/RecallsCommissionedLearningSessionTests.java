package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecallsCommissionedLearningSessionTests extends RecallsControllerTestBase {

  @Nested
  class CommissionedLearningSessionFeed {
    @Test
    void shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note note = makeMe.aNote().notebook(spanishConversationNotebook()).please();
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
      Notebook notebook = spanishConversationNotebook();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(currentTime).please();

      assertThat(controller.recalling("Asia/Shanghai", 0).getDueCommissioned(), hasSize(1));

      learningSessionController.commission(commissionRequest(notebook), "Asia/Shanghai");

      DueMemoryTrackers afterCommission = controller.recalling("Asia/Shanghai", 0);
      assertThat(afterCommission.getDueCommissioned(), hasSize(0));
      assertThat(afterCommission.getToRepeat(), hasSize(0));
    }

    @Test
    void returnsAwaitingReportSessionsAfterCommission() throws UnexpectedNoAccessRightException {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Notebook notebook = spanishConversationNotebook();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(currentTime).please();

      learningSessionController.commission(commissionRequest(notebook), "Asia/Shanghai");

      DueMemoryTrackers afterCommission = controller.recalling("Asia/Shanghai", 0);
      assertThat(afterCommission.getAwaitingReportSessions(), hasSize(1));
      assertEquals(
          "Spanish conversation",
          afterCommission.getAwaitingReportSessions().get(0).getNotebookName());
      assertEquals(
          notebook.getId(), afterCommission.getAwaitingReportSessions().get(0).getNotebookId());
      assertThat(
          afterCommission.getAwaitingReportSessions().get(0).getRequestMarkdown(),
          containsString("### Hola"));
    }

    @Test
    void dayThreeDueCommissionedOnlyGraciasAfterRecordedScores()
        throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").please());
      Timestamp dayOne = makeMe.aTimestamp().of(0, 8).please();
      Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
      Timestamp dayThree = makeMe.aTimestamp().of(2, 9).please();
      testabilitySettings.timeTravelTo(dayOne);

      Notebook notebook = spanishConversationNotebook();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      Note gracias = makeMe.aNote().notebook(notebook).title("Gracias").please();
      MemoryTracker holaTracker =
          makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayOne).please();
      MemoryTracker graciasTracker =
          makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dayOne).please();

      testabilitySettings.timeTravelTo(dayTwo);
      learningSessionController.record(
          recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

      testabilitySettings.timeTravelTo(dayThree);
      DueMemoryTrackers due = controller.recalling("Asia/Shanghai", 0);

      assertThat(due.getDueCommissioned(), hasSize(1));
      assertEquals(graciasTracker.getId(), due.getDueCommissioned().get(0).getMemoryTrackerId());
      assertTrue(holaTracker.getNextRecallAt().after(dayThree));
    }

    @Test
    void returnsRecordedSessionsAfterRecord() throws UnexpectedNoAccessRightException {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Notebook notebook = spanishConversationNotebook();
      Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
      Note gracias = makeMe.aNote().notebook(notebook).title("Gracias").please();
      makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(currentTime).please();
      makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(currentTime).please();

      learningSessionController.record(
          recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

      DueMemoryTrackers afterRecord = controller.recalling("Asia/Shanghai", 0);
      assertThat(afterRecord.getRecordedSessions(), hasSize(1));
      assertEquals(
          "Spanish conversation", afterRecord.getRecordedSessions().get(0).getNotebookName());
      assertEquals(notebook.getId(), afterRecord.getRecordedSessions().get(0).getNotebookId());
      assertThat(
          afterRecord.getRecordedSessions().get(0).getRequestMarkdown(),
          containsString("### Hola"));
      assertThat(afterRecord.getAwaitingReportSessions(), hasSize(0));
    }

    @Test
    void recordedSessionsDoesNotLeakAcrossUsers() throws UnexpectedNoAccessRightException {
      User userA = currentUser.getUser();
      User userB = makeMe.aUser().please();
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);

      Notebook notebookA = spanishConversationNotebook(userA);
      Note holaA = makeMe.aNote().notebook(notebookA).title("Hola").please();
      Note graciasA = makeMe.aNote().notebook(notebookA).title("Gracias").please();
      makeMe.aMemoryTrackerFor(holaA).commissioned().nextRecallAt(currentTime).please();
      makeMe.aMemoryTrackerFor(graciasA).commissioned().nextRecallAt(currentTime).please();

      learningSessionController.record(
          recordRequest(notebookA, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

      currentUser.setUser(userB);
      DueMemoryTrackers dueForB = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueForB.getRecordedSessions(), hasSize(0));
    }

    @Test
    void awaitingReportExclusionDoesNotLeakAcrossUsers() throws UnexpectedNoAccessRightException {
      User userA = currentUser.getUser();
      User userB = makeMe.aUser().please();
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);

      Notebook notebookA = spanishConversationNotebook(userA);
      Note holaA = makeMe.aNote().notebook(notebookA).title("Hola").please();
      makeMe.aMemoryTrackerFor(holaA).commissioned().nextRecallAt(currentTime).please();

      Notebook notebookB = makeMe.aNotebook().creatorAndOwner(userB).name("Kanji").please();
      Note noteB = makeMe.aNote().notebook(notebookB).title("水").please();
      MemoryTracker trackerB =
          makeMe.aMemoryTrackerFor(noteB).commissioned().nextRecallAt(currentTime).please();

      learningSessionController.commission(commissionRequest(notebookA), "Asia/Shanghai");

      currentUser.setUser(userB);
      DueMemoryTrackers dueForB = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueForB.getDueCommissioned(), hasSize(1));
      assertEquals(trackerB.getId(), dueForB.getDueCommissioned().get(0).getMemoryTrackerId());
    }
  }
}
