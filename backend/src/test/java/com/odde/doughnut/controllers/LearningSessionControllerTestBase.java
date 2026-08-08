package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.LearningSessionRepository;
import com.odde.doughnut.entities.repositories.SessionItemRepository;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class LearningSessionControllerTestBase extends ControllerTestBase {

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
    RecordLearningSessionRequest request = new RecordLearningSessionRequest();
    request.notebookId = notebook.getId();
    request.reportMarkdown = reportMarkdown;
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
}
