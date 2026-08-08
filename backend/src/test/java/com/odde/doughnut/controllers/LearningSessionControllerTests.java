package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionControllerTests extends LearningSessionControllerTestBase {

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
}
