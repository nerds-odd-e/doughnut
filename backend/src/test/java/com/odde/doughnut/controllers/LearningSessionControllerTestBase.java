package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.RecallLogRepository;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class LearningSessionControllerTestBase extends ControllerTestBase {

  protected static final String HOLA4_GRACIAS1_REPORT =
      """
      # Learning Session Report

      Hola: 4
      Gracias: 1
      """;

  protected static final String HOLA3_GRACIAS1_REPORT =
      """
      # Learning Session Report

      Hola: 3
      Gracias: 1
      """;

  protected static final String HOLA3_GRACIAS3_REPORT =
      """
      # Learning Session Report

      Hola: 3
      Gracias: 3
      """;

  @Autowired LearningSessionController controller;
  @Autowired RecallLogRepository recallLogRepository;

  @BeforeEach
  void setupLearningSessionTests() {
    currentUser.setUser(makeMe.aUser().please());
  }

  protected static String learningSessionReport(String title, int grade) {
    return "# Learning Session Report\n\n%s: %d\n".formatted(title, grade);
  }

  protected RecordLearningSessionRequest recordRequest(Notebook notebook, String reportMarkdown) {
    RecordLearningSessionRequest request = new RecordLearningSessionRequest();
    request.notebookId = notebook.getId();
    request.reportMarkdown = reportMarkdown;
    return request;
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
}
