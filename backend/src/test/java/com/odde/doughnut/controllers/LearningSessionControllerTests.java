package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionControllerTests extends ControllerTestBase {

  @Autowired LearningSessionController controller;
  @Autowired LearningSessionRepository learningSessionRepository;
  @Autowired SessionItemRepository sessionItemRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

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

      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = notebook.getId();

      LearningSessionCommissionResponse response = controller.commission(request, "Asia/Shanghai");

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
      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = foreignNotebook.getId();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.commission(request, "Asia/Shanghai"));
    }

    @Test
    void emptyDueCommissionedTrackers() {
      Notebook notebook =
          makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Empty notebook").please();
      makeMe.aNote().notebook(notebook).title("Lonely").content("Alone").please();

      long sessionsBefore = learningSessionRepository.count();

      CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
      request.notebookId = notebook.getId();

      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.commission(request, "Asia/Shanghai"));
      assertThat(ex.getStatusCode().value(), equalTo(400));
      assertThat(learningSessionRepository.count(), equalTo(sessionsBefore));
    }
  }
}
