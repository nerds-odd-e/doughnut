package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
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

  @Autowired LearningSessionController controller;
  @Autowired LearningSessionRepository learningSessionRepository;
  @Autowired SessionItemRepository sessionItemRepository;

  @BeforeEach
  void setupLearningSessionTests() {
    currentUser.setUser(makeMe.aUser().please());
  }

  protected RecordLearningSessionRequest recordRequest(Notebook notebook, String reportMarkdown) {
    RecordLearningSessionRequest request = new RecordLearningSessionRequest();
    request.notebookId = notebook.getId();
    request.reportMarkdown = reportMarkdown;
    return request;
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

  protected MemoryTracker trackerForNote(Notebook notebook, String title) {
    LearningSession session =
        learningSessionRepository
            .findByUser_IdAndNotebook_Id(currentUser.getUser().getId(), notebook.getId())
            .getFirst();
    return sessionItemFor(session.getId(), title).getMemoryTracker();
  }

  protected SessionItem sessionItemFor(int sessionId, String title) {
    return sessionItemRepository.findByLearningSession_Id(sessionId).stream()
        .filter(item -> item.getNoteTitle().equals(title))
        .findFirst()
        .orElseThrow();
  }
}
