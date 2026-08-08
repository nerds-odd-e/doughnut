package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionCommissionTests extends LearningSessionControllerTestBase {

  @Test
  void commissionsSpanishNotebookWithDueCommissionedTrackers()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);

    LearningSessionCommissionResponse response =
        controller.commission(commissionRequest(notebook), "Asia/Shanghai");

    assertThat(response.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
    assertThat(response.getLearningSessionId(), greaterThan(0));

    String markdown = response.getRequestMarkdown();
    assertThat(markdown, containsString("# Learning Session Request"));
    assertThat(markdown, containsString("<instructions>"));
    assertThat(markdown, containsString("</instructions>"));
    assertThat(
        markdown,
        containsString("You are the tutor to help the learner to study Spanish conversation."));
    assertThat(
        markdown,
        containsString("Wait for the learner's instruction before starting the learning session."));
    assertThat(markdown, not(containsString("Focus on conversational phrases")));
    assertThat(markdown, containsString("<session_item_titles>"));
    assertThat(markdown, containsString("- Hola\n"));
    assertThat(markdown, containsString("- Gracias\n"));
    assertThat(markdown, containsString("</session_item_titles>"));
    assertThat(markdown, containsString("<session_items>"));
    assertThat(markdown, containsString("### Hola"));
    assertThat(markdown, containsString("### Gracias"));
    assertThat(markdown, containsString("Expected learning content: Hello"));
    assertThat(markdown, containsString("Expected learning content: Thank you"));
    assertThat(markdown, containsString("not yet tutored"));
    assertThat(markdown, containsString("</session_items>"));
    assertThat(markdown, containsString("<how_to_report>"));
    assertThat(
        markdown,
        containsString(
            "Teach the session items above, then return a Learning Session Report giving one"));
    assertThat(markdown, containsString("score from 0 to 5 per item"));
    assertThat(markdown, containsString("Example of how to provide feedback:"));
    assertThat(markdown, containsString("# Learning Session Report\n\nHola: 5\nGracias: 1"));
    assertThat(
        markdown,
        containsString(
            "Only score session items that were actually taught in this session. Do not list"));
    assertThat(markdown, containsString("items that were not learnt in the session."));
    assertThat(markdown, containsString("</how_to_report>"));
    assertThat(markdown, containsString("- 5 — mastered the learning point with full fluency"));
    assertThat(markdown, containsString("- 4 — mastered the learning point with fluency"));
    assertThat(markdown, containsString("- 3 — mastered the learning point, but not fluent"));
    assertThat(
        markdown,
        containsString("- 2 — needed a reminder at first, then showed signs of mastering it"));
    assertThat(markdown, containsString("- 1 — needed several reminders"));
    assertThat(markdown, containsString("- 0 — could not reach the learning point even with help"));

    assertThat(
        sessionItemRepository.findByLearningSession_Id(response.getLearningSessionId()),
        hasSize(2));
    assertThat(learningSessionRepository.count(), equalTo(1L));
  }

  @Test
  void requestMarkdownIncludesNotebookQuestionGenerationInstruction()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    makeMe
        .theNotebook(notebook)
        .readmeContent(
            "---\nquestion_generation_instruction: Focus on conversational phrases.\n---\n")
        .please();

    LearningSessionCommissionResponse response =
        controller.commission(commissionRequest(notebook), "Asia/Shanghai");

    String markdown = response.getRequestMarkdown();
    assertThat(
        markdown,
        containsString(
            "<instructions>\nYou are the tutor to help the learner to study Spanish"
                + " conversation.\n\nFocus on conversational phrases.\n\nWait for the learner's"
                + " instruction before starting the learning session.\n</instructions>"));
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

    Notebook notebook = spanishNotebook(dayTwo);

    LearningSessionCommissionResponse first =
        controller.commission(commissionRequest(notebook), "Asia/Shanghai");
    Integer abandonedId = first.getLearningSessionId();

    LearningSessionCommissionResponse second =
        controller.commission(commissionRequest(notebook), "Asia/Shanghai");

    assertThat(second.getLearningSessionId(), not(equalTo(abandonedId)));
    assertThat(
        learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
            currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.AWAITING_REPORT),
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

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);

    LearningSession recordedSession = recordedLearningSession(fixture.notebook(), priorSessionAt);
    addRecordedFeedback(recordedSession, fixture.holaTracker(), 4, priorSessionAt);

    LearningSessionCommissionResponse response =
        controller.commission(commissionRequest(fixture.notebook()), "Asia/Shanghai");

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

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);

    LearningSession recordedSession = recordedLearningSession(fixture.notebook(), priorSessionAt);
    addRecordedFeedback(recordedSession, fixture.holaTracker(), 3, priorSessionAt);
    Integer recordedSessionId = recordedSession.getId();

    controller.commission(commissionRequest(fixture.notebook()), "Asia/Shanghai");

    assertTrue(learningSessionRepository.findById(recordedSessionId).isPresent());
    assertThat(
        learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
            currentUser.getUser().getId(),
            fixture.notebook().getId(),
            LearningSessionStatus.RECORDED),
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
