package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class RecallsControllerTestBase extends ControllerTestBase {

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

  @Autowired RecallsController controller;
  @Autowired LearningSessionController learningSessionController;

  @BeforeEach
  void setupRecallsTests() {
    currentUser.setUser(makeMe.aUser().please());
  }

  protected Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  protected MemoryTracker dueTracker(Note note, Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerFor(note).nextRecallAt(nextRecallAt).please();
  }

  protected MemoryTracker dueTracker(Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(nextRecallAt).please();
  }

  protected CommissionLearningSessionRequest commissionRequest(Notebook notebook) {
    CommissionLearningSessionRequest request = new CommissionLearningSessionRequest();
    request.notebookId = notebook.getId();
    return request;
  }

  protected RecordLearningSessionRequest recordRequest(Notebook notebook, String reportMarkdown) {
    RecordLearningSessionRequest request = new RecordLearningSessionRequest();
    request.notebookId = notebook.getId();
    request.reportMarkdown = reportMarkdown;
    return request;
  }

  protected Notebook spanishConversationNotebook() {
    return spanishConversationNotebook(currentUser.getUser());
  }

  protected Notebook spanishConversationNotebook(User owner) {
    return makeMe.aNotebook().creatorAndOwner(owner).name("Spanish conversation").please();
  }
}
