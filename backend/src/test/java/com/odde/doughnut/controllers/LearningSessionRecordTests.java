package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearningSessionRecordTests extends LearningSessionControllerTestBase {

  @Test
  void recordsSpanishNotebookSessionWithMatchedScores() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);

    RecordLearningSessionResponse response =
        controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

    assertThat(response.getRecordedAt(), equalTo(dayTwo));
    assertThat(response.getRecordedItems(), hasSize(2));
    assertThat(response.getRejectedEntries(), empty());

    LearningSession session =
        learningSessionRepository
            .findByUser_IdAndNotebook_Id(currentUser.getUser().getId(), notebook.getId())
            .getFirst();
    assertThat(session.getRecordedAt(), equalTo(dayTwo));

    for (SessionItem item : sessionItemRepository.findByLearningSession_Id(session.getId())) {
      assertThat(item.getFeedbackRecordedAt(), equalTo(dayTwo));
      assertThat(item.getMemoryTracker().getRecallCount(), equalTo(1));
      assertThat(item.getMemoryTracker().getLastRecalledAt(), equalTo(dayTwo));
    }
  }

  @Test
  void allLinesRejectedCreatesNoSession() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    Notebook notebook = fixture.notebook();
    MemoryTracker holaTracker = fixture.holaTracker();
    var trackerStateBefore =
        List.of(
            holaTracker.getLastRecalledAt(),
            holaTracker.getRecallCount(),
            holaTracker.getStability(),
            holaTracker.getNextRecallAt());
    long sessionsBefore = learningSessionRepository.count();

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

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(2));
    assertThat(learningSessionRepository.count(), equalTo(sessionsBefore));
    assertThat(
        List.of(
            holaTracker.getLastRecalledAt(),
            holaTracker.getRecallCount(),
            holaTracker.getStability(),
            holaTracker.getNextRecallAt()),
        equalTo(trackerStateBefore));
  }

  @Test
  void rejectsTitleWithoutCommissionedTracker() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 5)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(1));
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("No commissioned memory tracker"));
  }
}
