package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Grade;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class LearningSessionRecordTutorFeedbackRecallLogTests extends LearningSessionControllerTestBase {

  @Autowired MemoryTrackerController memoryTrackerController;

  @ParameterizedTest
  @CsvSource({"4, EASY", "3, GOOD", "2, HARD", "1, AGAIN"})
  void matchedGradeLeavesMappedRecallLog(int gradeValue, Grade grade)
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", gradeValue)),
        "Asia/Shanghai");

    List<RecallLog> logs = memoryTrackerController.getRecallLogs(fixture.holaTracker());
    RecallLog log = logs.get(0);
    assertThat(log.getGrade(), is(grade));
    assertThat(log.getAnswerId(), nullValue());
  }

  @Test
  void recordingWritesTutorFeedbackOnRecallLog() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(
            fixture.notebook(),
            sessionItemFeedbackReport(
                "Hola", 4, "Pronunciation was clear; still mixes ser/estar under pressure.")),
        "Asia/Shanghai");

    RecallLog log = memoryTrackerController.getRecallLogs(fixture.holaTracker()).get(0);
    assertThat(
        log.getTutorFeedback(),
        is("Pronunciation was clear; still mixes ser/estar under pressure."));
  }

  @Test
  void legacyGradeOnlyReportLeavesTutorFeedbackNull() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", 4)), "Asia/Shanghai");

    RecallLog log = memoryTrackerController.getRecallLogs(fixture.holaTracker()).get(0);
    assertThat(log.getTutorFeedback(), nullValue());
  }

  @Test
  void unmatchedTitleWritesNoRecallLog() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("UnknownNote", 4)),
        "Asia/Shanghai");

    assertThat(memoryTrackerController.getRecallLogs(fixture.holaTracker()), empty());
    assertThat(memoryTrackerController.getRecallLogs(fixture.graciasTracker()), empty());
  }
}
