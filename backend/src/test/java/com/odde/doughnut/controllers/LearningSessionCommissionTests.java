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

  private CommissionLearningSessionRequest commissionRequest(Notebook notebook) {
    CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
    request.notebookId = notebook.getId();
    return request;
  }

  private LearningSession recordedLearningSession(Notebook notebook, Timestamp at) {
    return makeMe
        .aLearningSession()
        .forNotebook(notebook)
        .by(currentUser.getUser())
        .status(LearningSessionStatus.RECORDED)
        .recordedAt(at)
        .please();
  }

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
    assertThat(response.getRequestMarkdown(), containsString("# Learning Session Request"));
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
