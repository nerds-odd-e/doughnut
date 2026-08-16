package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.utils.TimestampOperations;
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
  void firstScoreFourOnNewPersistsDifficultyFive() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    assertThat(fixture.holaTracker().getDifficulty(), nullValue());

    controller.record(recordRequest(fixture.notebook(), HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    assertThat(fixture.holaTracker().getDifficulty(), equalTo(5f));
  }

  @Test
  void firstScoreFiveOnNewPersistsDifficultyFiveAndStability24()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(recordRequest(fixture.notebook(), HOLA_GRACIAS_REPORT), "Asia/Shanghai");

    MemoryTracker hola = fixture.holaTracker();
    assertThat(hola.getDifficulty(), equalTo(5f));
    assertThat(hola.getStability(), equalTo(24f));
  }

  @Test
  void onTimeSecondScoreFourPersistsStability102() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreFour().holaTracker().getStability(), equalTo(102f));
  }

  @Test
  void onTimeSecondScoreFivePersistsStability169() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreFive().holaTracker().getStability(), equalTo(169f));
  }

  @Test
  void onTimeSecondScoreFourPersistsGoodNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreFour().holaTracker().getDifficulty(), equalTo(5.0014133f));
  }

  @Test
  void onTimeSecondScoreFivePersistsEasyNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreFive().holaTracker().getDifficulty(), equalTo(1.9850327f));
  }

  @Test
  void overdueSecondScoreFourGrowsStabilityMoreThanOnTime()
      throws UnexpectedNoAccessRightException {
    SpanishNotebookFixture fixture = afterStaggeredSecondScore(4);
    assertThat(
        fixture.graciasTracker().getStability(), greaterThan(fixture.holaTracker().getStability()));
    assertThat(fixture.graciasTracker().getStability(), equalTo(146f));
  }

  @Test
  void overdueSecondScoreFiveGrowsStabilityMoreThanOnTime()
      throws UnexpectedNoAccessRightException {
    assertThat(afterStaggeredSecondScore(5).graciasTracker().getStability(), equalTo(253f));
  }

  @Test
  void firstScoreOneLeavesDifficultyUnset() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(recordRequest(fixture.notebook(), HOLA_GRACIAS_REPORT), "Asia/Shanghai");

    assertThat(fixture.graciasTracker().getDifficulty(), nullValue());
  }

  @Test
  void scoreZeroSchedulesStrictlyAfterRecordedAt() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = spanishNotebook(dayTwo);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 0)), "Asia/Shanghai");

    MemoryTracker holaTracker = trackerForNote(notebook, "Hola");
    assertTrue(holaTracker.getNextRecallAt().after(response.getRecordedAt()));
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

  private SpanishNotebookFixture afterOnTimeSecondScoreFour()
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(HOLA4_GRACIAS1_REPORT);
  }

  private SpanishNotebookFixture afterOnTimeSecondScoreFive()
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(HOLA_GRACIAS_REPORT);
  }

  private SpanishNotebookFixture afterOnTimeSecondScore(String secondReport)
      throws UnexpectedNoAccessRightException {
    Timestamp firstRecord = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(firstRecord);

    SpanishNotebookFixture fixture = spanishNotebookFixture(firstRecord);
    controller.record(recordRequest(fixture.notebook(), HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    Timestamp secondRecord = TimestampOperations.addHoursToTimestamp(firstRecord, 24);
    testabilitySettings.timeTravelTo(secondRecord);
    controller.record(recordRequest(fixture.notebook(), secondReport), "Asia/Shanghai");

    return fixture;
  }

  private SpanishNotebookFixture afterStaggeredSecondScore(int score)
      throws UnexpectedNoAccessRightException {
    Timestamp firstRecord = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(firstRecord);

    SpanishNotebookFixture fixture = spanishNotebookFixture(firstRecord);
    controller.record(recordRequest(fixture.notebook(), HOLA4_GRACIAS4_REPORT), "Asia/Shanghai");

    testabilitySettings.timeTravelTo(TimestampOperations.addHoursToTimestamp(firstRecord, 24));
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");

    testabilitySettings.timeTravelTo(TimestampOperations.addHoursToTimestamp(firstRecord, 48));
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Gracias", score)),
        "Asia/Shanghai");

    return fixture;
  }
}
