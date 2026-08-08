package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class LearningSessionControllerTestBase extends ControllerTestBase {

  protected static final String HOLA_GRACIAS_REPORT =
      """
      # Learning Session Report

      Hola: 5
      Gracias: 1
      """;

  protected static final String HOLA4_GRACIAS1_REPORT =
      """
      # Learning Session Report

      Hola: 4
      Gracias: 1
      """;

  protected static final String GRACIAS4_REPORT =
      """
      # Learning Session Report

      Gracias: 4
      """;

  @Autowired LearningSessionController controller;
  @Autowired LearningSessionRepository learningSessionRepository;
  @Autowired SessionItemRepository sessionItemRepository;

  @BeforeEach
  void setupLearningSessionTests() {
    currentUser.setUser(makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").please());
  }

  protected CommissionLearningSessionRequest commissionRequest(Notebook notebook) {
    CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
    request.notebookId = notebook.getId();
    return request;
  }

  protected RecordLearningSessionRequest recordRequest(Notebook notebook, String reportMarkdown) {
    return recordRequest(notebook, reportMarkdown, null);
  }

  protected RecordLearningSessionRequest recordRequest(
      Notebook notebook, String reportMarkdown, Integer learningSessionId) {
    RecordLearningSessionRequest request = new RecordLearningSessionRequest();
    request.notebookId = notebook.getId();
    request.reportMarkdown = reportMarkdown;
    request.learningSessionId = learningSessionId;
    return request;
  }

  protected LearningSession recordedLearningSession(Notebook notebook, Timestamp at) {
    return makeMe
        .aLearningSession()
        .forNotebook(notebook)
        .by(currentUser.getUser())
        .status(LearningSessionStatus.RECORDED)
        .commissionedAt(at)
        .recordedAt(at)
        .please();
  }

  protected void addRecordedFeedback(
      LearningSession session, MemoryTracker tracker, int score, Timestamp at) {
    makeMe
        .aSessionItem()
        .learningSession(session)
        .memoryTracker(tracker)
        .feedbackScore(score)
        .feedbackRecordedAt(at)
        .please();
  }

  protected record SpanishNotebookFixture(
      Notebook notebook, MemoryTracker holaTracker, MemoryTracker graciasTracker) {}

  protected SpanishNotebookFixture spanishNotebookFixture(Timestamp dueAt) {
    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
    Note gracias = makeMe.aNote().notebook(notebook).title("Gracias").content("Thank you").please();
    MemoryTracker holaTracker =
        makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dueAt).please();
    MemoryTracker graciasTracker =
        makeMe.aMemoryTrackerFor(gracias).commissioned().nextRecallAt(dueAt).please();
    return new SpanishNotebookFixture(notebook, holaTracker, graciasTracker);
  }

  protected Notebook spanishNotebook(Timestamp dueAt) {
    return spanishNotebookFixture(dueAt).notebook();
  }

  protected LearningSession commissionAndRecordSpanishNotebook(Timestamp dueAt)
      throws UnexpectedNoAccessRightException {
    Notebook notebook = spanishNotebook(dueAt);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");
    controller.record(recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");
    return learningSessionRepository
        .findByUser_IdAndNotebook_IdAndStatus(
            currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
        .getFirst();
  }

  protected MemoryTracker trackerForNote(Notebook notebook, String title) {
    LearningSession session =
        learningSessionRepository
            .findByUser_IdAndNotebook_IdAndStatus(
                currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
            .getFirst();
    return sessionItemFor(session.getId(), title).getMemoryTracker();
  }

  protected SessionItem sessionItemFor(int sessionId, String title) {
    return sessionItemRepository.findByLearningSession_Id(sessionId).stream()
        .filter(item -> item.getNoteTitle().equals(title))
        .findFirst()
        .orElseThrow();
  }

  protected record RecordedAndAwaitingSessions(
      Notebook notebook, LearningSession recordedSession, LearningSession awaitingSession) {}

  protected RecordedAndAwaitingSessions commissionRecordAndRecommission(
      Timestamp commissionAt, Timestamp recommissionAt) throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(commissionAt);
    Notebook notebook = spanishNotebook(commissionAt);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");
    controller.record(recordRequest(notebook, HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    LearningSession recordedSession =
        learningSessionRepository
            .findByUser_IdAndNotebook_IdAndStatus(
                currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
            .getFirst();

    testabilitySettings.timeTravelTo(recommissionAt);
    LearningSession awaitingSession =
        makeMe
            .aLearningSession()
            .forNotebook(notebook)
            .by(currentUser.getUser())
            .status(LearningSessionStatus.AWAITING_REPORT)
            .commissionedAt(recommissionAt)
            .please();
    for (SessionItem recordedItem :
        sessionItemRepository.findByLearningSession_Id(recordedSession.getId())) {
      makeMe
          .aSessionItem()
          .learningSession(awaitingSession)
          .memoryTracker(recordedItem.getMemoryTracker())
          .please();
    }
    return new RecordedAndAwaitingSessions(notebook, recordedSession, awaitingSession);
  }
}
