package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecordLearningSessionResponse;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearningSessionRecordTests extends LearningSessionControllerTestBase {

  @Test
  void recordsMatchedGradesAsRecallLogsAndSchedulesTrackers()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(fixture.notebook(), HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    assertThat(response.getRecordedAt(), equalTo(dayTwo));
    assertThat(response.getRecordedItems(), hasSize(2));
    assertThat(response.getRejectedEntries(), empty());

    for (MemoryTracker tracker : List.of(fixture.holaTracker(), fixture.graciasTracker())) {
      assertThat(
          recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(tracker.getId()),
          hasSize(1));
      assertThat(tracker.getLastRecalledAt(), equalTo(dayTwo));
    }
  }

  @Test
  void allLinesRejectedWritesNoRecallLogs() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    MemoryTracker holaTracker = fixture.holaTracker();
    TrackerLearningState trackerStateBefore = learningStateOf(holaTracker);
    long logsBefore = recallLogRepository.count();

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(
                fixture.notebook(),
                """
                # Learning Session Report

                UnknownNote: 3
                Hola: six
                """),
            "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(2));
    assertThat(recallLogRepository.count(), equalTo(logsBefore));
    assertThat(learningStateOf(holaTracker), equalTo(trackerStateBefore));
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
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(1));
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("No commissioned memory tracker"));
  }

  @Test
  void rejectsTrashedCommissionedNoteWithoutChangingItsLearningHistoryOrSchedule()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Note note = makeMe.aNote().folder(trash).title("Hola").content("Hello").please();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(dayTwo).please();
    TrackerLearningState trackerStateBefore = learningStateOf(tracker);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(1));
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("No commissioned memory tracker"));
    assertThat(
        recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(tracker.getId()),
        empty());
    assertThat(learningStateOf(tracker), equalTo(trackerStateBefore));
  }

  @Test
  void legacyScoresTagReportRecordsGrades() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(fixture.notebook(), legacyScoresTaggedReport("Hola: 4\nGracias: 1\n")),
            "Asia/Shanghai");

    assertThat(response.getRejectedEntries(), empty());
    assertThat(response.getRecordedItems(), hasSize(2));
    assertThat(response.getRecordedItems().get(0).getGrade(), equalTo(4));
    assertThat(response.getRecordedItems().get(1).getGrade(), equalTo(1));
    assertThat(fixture.holaTracker().getLastRecalledAt(), equalTo(dayTwo));
  }

  private static TrackerLearningState learningStateOf(MemoryTracker tracker) {
    return new TrackerLearningState(
        tracker.getLastRecalledAt(),
        tracker.getRecallCount(),
        tracker.getStability(),
        tracker.getDifficulty(),
        tracker.getNextRecallAt());
  }

  private record TrackerLearningState(
      Timestamp lastRecalledAt,
      Integer recallCount,
      Float stability,
      Float difficulty,
      Timestamp nextRecallAt) {}
}
