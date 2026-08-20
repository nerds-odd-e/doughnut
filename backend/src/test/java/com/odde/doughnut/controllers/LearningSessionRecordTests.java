package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.Arrays;
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
    var trackerStateBefore =
        Arrays.asList(
            holaTracker.getLastRecalledAt(),
            holaTracker.getRecallCount(),
            holaTracker.getStability(),
            holaTracker.getNextRecallAt());
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
    assertThat(
        Arrays.asList(
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
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(1));
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("No commissioned memory tracker"));
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
}
