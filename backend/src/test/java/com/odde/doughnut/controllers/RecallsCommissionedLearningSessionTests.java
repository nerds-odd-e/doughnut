package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
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
    void dayThreeDueCommissionedHolaAndGraciasAfterRecordedScores()
        throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.aUser().please());
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
      DueMemoryTrackers due = controller.recalling("UTC", 0);

      assertThat(
          due.getDueCommissioned().stream().map(item -> item.getMemoryTrackerId()).toList(),
          containsInAnyOrder(holaTracker.getId(), graciasTracker.getId()));
    }
  }
}
