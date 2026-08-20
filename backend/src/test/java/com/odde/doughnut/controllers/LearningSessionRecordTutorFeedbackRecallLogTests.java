package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
