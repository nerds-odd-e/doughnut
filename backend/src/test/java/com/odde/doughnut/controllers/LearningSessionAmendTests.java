package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.NoteRecallInfo;
import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LearningSessionAmendTests extends LearningSessionControllerTestBase {

  @Autowired NoteController noteController;

  @Test
  void partialAmendUpdatesGraciasScore() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    LearningSession session = commissionAndRecordSpanishNotebook(dayTwo);

    Timestamp dayTwoLater = makeMe.aTimestamp().of(1, 10).please();
    testabilitySettings.timeTravelTo(dayTwoLater);

    RecordLearningSessionResponse amendResponse =
        controller.record(recordRequest(session.getNotebook(), GRACIAS4_REPORT), "Asia/Shanghai");

    assertThat(amendResponse.getRecordedItems().getFirst().getNoteTitle(), equalTo("Gracias"));
    assertThat(amendResponse.getRecordedItems().getFirst().getScore(), equalTo(4));

    assertThat(sessionItemFor(session.getId(), "Gracias").getFeedbackScore(), equalTo(4));
  }

  @Test
  void amendUpdatesLatestTutorFeedbackScore() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    LearningSession session = commissionAndRecordSpanishNotebook(dayTwo);
    controller.record(recordRequest(session.getNotebook(), GRACIAS4_REPORT), "Asia/Shanghai");

    Note graciasNote = trackerForNote(session.getNotebook(), "Gracias").getNote();
    NoteRecallInfo noteInfo = noteController.getNoteInfo(graciasNote);
    MemoryTracker commissioned =
        noteInfo.getMemoryTrackers().stream()
            .filter(MemoryTracker::isCommissioned)
            .findFirst()
            .orElseThrow();
    assertThat(commissioned.getLatestTutorFeedbackScore(), equalTo(4));
  }

  @Test
  void partialAmendUpdatesOnlyMatchedItem() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    LearningSession session = commissionAndRecordSpanishNotebook(dayTwo);

    RecordLearningSessionResponse amendResponse =
        controller.record(
            recordRequest(
                session.getNotebook(),
                """
                # Learning Session Report

                Hola: 5
                """),
            "Asia/Shanghai");

    assertThat(amendResponse.getRecordedItems().getFirst().getNoteTitle(), equalTo("Hola"));
    assertThat(amendResponse.getRecordedItems().getFirst().getScore(), equalTo(5));

    assertThat(sessionItemFor(session.getId(), "Hola").getFeedbackScore(), equalTo(5));
    assertThat(sessionItemFor(session.getId(), "Gracias").getFeedbackScore(), equalTo(1));
  }

  @Test
  void allRejectedAmendLeavesPriorFeedback() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    LearningSession session = commissionAndRecordSpanishNotebook(dayTwo);
    Timestamp originalRecordedAt = session.getRecordedAt();

    RecordLearningSessionResponse amendResponse =
        controller.record(
            recordRequest(
                session.getNotebook(),
                """
                # Learning Session Report

                UnknownNote: 3
                """),
            "Asia/Shanghai");

    assertThat(amendResponse.getRecordedAt(), equalTo(originalRecordedAt));
    assertThat(amendResponse.getRejectedEntries(), hasSize(1));

    assertThat(sessionItemFor(session.getId(), "Hola").getFeedbackScore(), equalTo(4));
    assertThat(sessionItemFor(session.getId(), "Gracias").getFeedbackScore(), equalTo(1));
  }

  @Test
  void recordWithoutLearningSessionIdPrefersAwaitingOverRecordedAmend()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    Timestamp dayTwoLater = makeMe.aTimestamp().of(1, 10).please();
    RecordedAndAwaitingSessions sessions = commissionRecordAndRecommission(dayTwo, dayTwoLater);
    Integer recordedSessionId = sessions.recordedSession().getId();

    assertThat(sessionItemFor(recordedSessionId, "Gracias").getFeedbackScore(), equalTo(1));

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(
                sessions.notebook(),
                """
                # Learning Session Report

                Gracias: 4
                """),
            "Asia/Shanghai");

    assertThat(response.getRecordedItems().getFirst().getNoteTitle(), equalTo("Gracias"));

    LearningSession awaitingAfter =
        learningSessionRepository.findById(sessions.awaitingSession().getId()).orElseThrow();
    assertThat(awaitingAfter.getStatus(), equalTo(LearningSessionStatus.RECORDED));

    assertThat(sessionItemFor(recordedSessionId, "Gracias").getFeedbackScore(), equalTo(1));
  }

  @Test
  void amendRejectsWhenPreSessionSnapshotMissing() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    Note hola = makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();
    MemoryTracker holaTracker =
        makeMe.aMemoryTrackerFor(hola).commissioned().nextRecallAt(dayTwo).please();

    LearningSession recordedSession = recordedLearningSession(notebook, dayTwo);
    addRecordedFeedback(recordedSession, holaTracker, 4, dayTwo);

    SessionItem legacyItem = sessionItemFor(recordedSession.getId(), "Hola");
    assertThat(legacyItem.getPreSessionRecallCount(), nullValue());

    RecordLearningSessionResponse amendResponse =
        controller.record(
            recordRequest(
                notebook,
                """
                # Learning Session Report

                Hola: 5
                """,
                recordedSession.getId()),
            "Asia/Shanghai");

    assertThat(amendResponse.getRejectedEntries(), hasSize(1));
    assertThat(
        amendResponse.getRejectedEntries().getFirst().getReason(),
        equalTo("Cannot amend: no pre-session snapshot for this item."));
    assertThat(legacyItem.getFeedbackScore(), equalTo(4));
  }

  @Test
  void amendWithLearningSessionIdWhileAwaitingSessionExists()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    Timestamp dayTwoLater = makeMe.aTimestamp().of(1, 10).please();
    RecordedAndAwaitingSessions sessions = commissionRecordAndRecommission(dayTwo, dayTwoLater);
    Integer recordedSessionId = sessions.recordedSession().getId();

    RecordLearningSessionResponse amendResponse =
        controller.record(
            recordRequest(
                sessions.notebook(),
                """
                # Learning Session Report

                Gracias: 4
                """,
                recordedSessionId),
            "Asia/Shanghai");

    assertThat(amendResponse.getRecordedItems().getFirst().getNoteTitle(), equalTo("Gracias"));
    assertThat(amendResponse.getRecordedItems().getFirst().getScore(), equalTo(4));

    LearningSession awaitingAfter =
        learningSessionRepository.findById(sessions.awaitingSession().getId()).orElseThrow();
    assertThat(awaitingAfter.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
    for (SessionItem item :
        sessionItemRepository.findByLearningSession_Id(sessions.awaitingSession().getId())) {
      assertThat(item.getFeedbackScore(), nullValue());
    }

    assertThat(sessionItemFor(recordedSessionId, "Gracias").getFeedbackScore(), equalTo(4));
  }

  @Test
  void highToLowAmendReschedulesFromSnapshot() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);
    controller.commission(commissionRequest(notebook), "Asia/Shanghai");
    controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");

    MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
    Timestamp scheduleAfterScoreFive = holaTracker.getNextRecallAt();

    controller.record(
        recordRequest(
            notebook,
            """
            # Learning Session Report

            Hola: 1
            """),
        "Asia/Shanghai");

    assertThat(holaTracker.getNextRecallAt(), lessThan(scheduleAfterScoreFive));
  }
}
