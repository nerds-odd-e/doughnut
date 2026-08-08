package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LearningSessionRecordTests extends LearningSessionControllerTestBase {

  @Test
  void recordsSpanishNotebookSessionWithMatchedScores() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");

    RecordLearningSessionResponse response =
        controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

    assertThat(response.getStatus(), equalTo(LearningSessionStatus.RECORDED));
    assertThat(response.getRecordedAt(), equalTo(dayTwo));
    assertThat(response.getRecordedItems(), hasSize(2));
    assertThat(response.getRejectedEntries(), empty());

    LearningSession session =
        learningSessionRepository
            .findByUser_IdAndNotebook_IdAndStatus(
                currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.RECORDED)
            .getFirst();
    assertThat(session.getRecordedAt(), equalTo(dayTwo));

    for (SessionItem item : sessionItemRepository.findByLearningSession_Id(session.getId())) {
      assertThat(item.getFeedbackScore(), notNullValue());
      assertThat(item.getFeedbackRecordedAt(), equalTo(dayTwo));
      assertThat(item.getMemoryTracker().getRecallCount(), equalTo(1));
    }
  }

  @Test
  void highScoreSchedulesLaterThanLowScoreFromSameStartingState()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");
    controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

    MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
    MemoryTracker graciasTracker = trackerForNote(notebook, "Gracias");

    assertThat(holaTracker.getNextRecallAt(), greaterThan(graciasTracker.getNextRecallAt()));
  }

  @Test
  void scoreZeroSchedulesStrictlyAfterRecordedAt() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, "# Learning Session Report\n\nHola: 0\n"), "Asia/Shanghai");

    MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
    assertTrue(holaTracker.getNextRecallAt().after(response.getRecordedAt()));
  }

  @Test
  void allLinesRejectedStaysAwaitingReport() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(
                notebook,
                """
                # Learning Session Report

                UnknownNote: 3
                Hola: six
                """),
            "Asia/Shanghai");

    assertThat(response.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(2));
    assertThat(
        learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
            currentUser.getUser().getId(), notebook.getId(), LearningSessionStatus.AWAITING_REPORT),
        hasSize(1));
  }

  @Test
  void notFoundWhenNoSessionToRecordOrAmend() {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Lonely").please();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.record(recordRequest(notebook, "Hola: 5\n"), "Asia/Shanghai"));
    assertThat(ex.getStatusCode().value(), equalTo(404));
    assertThat(
        ex.getReason(), equalTo("No learning session to record or amend for this notebook."));
  }

  @Test
  void initialRecordCapturesPreSessionSnapshot() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    LearningSession session = commissionAndRecordSpanishNotebook(dayTwo);

    for (SessionItem item : sessionItemRepository.findByLearningSession_Id(session.getId())) {
      assertThat(item.getPreSessionRecallCount(), equalTo(0));
      assertThat(item.getPreSessionForgettingCurveIndex(), notNullValue());
    }
  }
}
