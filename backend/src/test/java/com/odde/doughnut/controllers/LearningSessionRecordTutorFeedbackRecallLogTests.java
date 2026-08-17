package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.ProductOutcome;
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

  @Test
  void scoreFourLeavesAGoodRecallLogWithoutAnswer() throws UnexpectedNoAccessRightException {
    Timestamp assimilatedAt = makeMe.aTimestamp().of(1, 8).please();
    Timestamp recordedAt = makeMe.aTimestamp().of(2, 8).please();
    testabilitySettings.timeTravelTo(recordedAt);

    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note hola = makeMe.aNote().notebook(notebook).title("Hola").please();
    MemoryTracker holaTracker =
        makeMe
            .aMemoryTrackerFor(hola)
            .commissioned()
            .assimilatedAt(assimilatedAt)
            .nextRecallAt(recordedAt)
            .please();

    controller.record(recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    List<RecallLog> logs = memoryTrackerController.getRecallLogs(holaTracker);
    assertThat(logs, hasSize(1));
    RecallLog log = logs.get(0);
    assertThat(log.getProductOutcome(), is(ProductOutcome.GOOD));
    assertThat(log.getRecordedAt(), equalTo(recordedAt));
    assertThat(log.getElapsedHours(), equalTo(24));
    assertThat(log.getAnswerId(), nullValue());
    assertThat(log.getMemoryTrackerId(), equalTo(holaTracker.getId()));
  }

  @ParameterizedTest
  @CsvSource({"5, EASY", "3, HARD", "2, SHRINK", "1, AGAIN", "0, AGAIN_ZERO"})
  void matchedScoreLeavesMappedRecallLog(int score, ProductOutcome outcome)
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");

    List<RecallLog> logs = memoryTrackerController.getRecallLogs(fixture.holaTracker());
    assertThat(logs.get(0).getProductOutcome(), is(outcome));
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
