package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LearningSessionRecordTutorFeedbackTests extends LearningSessionControllerTestBase {

  @Test
  void firstScoreFourOnNewPersistsDifficultyFive() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    assertThat(fixture.holaTracker().getDifficulty(), nullValue());

    controller.record(recordRequest(fixture.notebook(), HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    assertThat(fixture.holaTracker().getDifficulty(), equalTo(5f));
  }

  @ParameterizedTest
  @CsvSource({"5", "3"})
  void firstScoreOnNewPersistsDifficultyFiveAndStability24(int score)
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");

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
  void onTimeSecondScoreThreePersistsStability71() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreThree().holaTracker().getStability(), equalTo(71f));
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
  void onTimeSecondScoreThreePersistsHardNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreThree().holaTracker().getDifficulty(), equalTo(8.0177937f));
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
  void overdueSecondScoreThreeGrowsStabilityMoreThanOnTime()
      throws UnexpectedNoAccessRightException {
    assertThat(afterStaggeredSecondScore(3).graciasTracker().getStability(), equalTo(97f));
  }

  @Test
  void onTimeSecondScoreOnePersistsAgainStability() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreOne().holaTracker().getStability(), equalTo(8f));
  }

  @Test
  void onTimeSecondScoreOnePersistsAgainNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScoreOne().holaTracker().getDifficulty(), equalTo(10f));
  }

  @Test
  void onTimeSecondScoreOneSchedulesDueFromStability() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = afterOnTimeSecondScoreOne().holaTracker();
    assertThat(
        hola.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                hola.getLastRecalledAt(), Math.round(hola.getStability()))));
  }

  @Test
  void onTimeSecondScoreZeroMatchesScoreOneSchedule() throws UnexpectedNoAccessRightException {
    SpanishNotebookFixture fixture =
        afterOnTimeSecondScore(
            HOLA4_GRACIAS4_REPORT,
            """
            # Learning Session Report

            Hola: 1
            Gracias: 0
            """);
    MemoryTracker scoreOne = fixture.holaTracker();
    MemoryTracker scoreZero = fixture.graciasTracker();

    assertThat(scoreZero.getStability(), equalTo(scoreOne.getStability()));
    assertThat(scoreZero.getDifficulty(), equalTo(scoreOne.getDifficulty()));
    assertThat(scoreZero.getNextRecallAt(), equalTo(scoreOne.getNextRecallAt()));
  }

  @Test
  void onTimeSecondScoreTwoShrinksStabilityAndLeavesDifficultyUnchanged()
      throws UnexpectedNoAccessRightException {
    MemoryTracker hola = afterOnTimeSecondScore(learningSessionReport("Hola", 2)).holaTracker();
    assertThat(hola.getStability(), equalTo(19f));
    assertThat(hola.getDifficulty(), equalTo(5f));
  }

  @ParameterizedTest
  @CsvSource({"1", "2"})
  void firstScoreLeavesDifficultyUnsetAndStabilityZero(int score)
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");

    assertThat(fixture.holaTracker().getDifficulty(), nullValue());
    assertThat(fixture.holaTracker().getStability(), equalTo(0f));
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

  private SpanishNotebookFixture afterOnTimeSecondScoreFour()
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(HOLA4_GRACIAS1_REPORT);
  }

  private SpanishNotebookFixture afterOnTimeSecondScoreFive()
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(HOLA_GRACIAS_REPORT);
  }

  private SpanishNotebookFixture afterOnTimeSecondScoreThree()
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(learningSessionReport("Hola", 3));
  }

  private SpanishNotebookFixture afterOnTimeSecondScoreOne()
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(learningSessionReport("Hola", 1));
  }

  private SpanishNotebookFixture afterOnTimeSecondScore(String secondReport)
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(HOLA4_GRACIAS1_REPORT, secondReport);
  }

  private SpanishNotebookFixture afterOnTimeSecondScore(String firstReport, String secondReport)
      throws UnexpectedNoAccessRightException {
    Timestamp firstRecord = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(firstRecord);

    SpanishNotebookFixture fixture = spanishNotebookFixture(firstRecord);
    controller.record(recordRequest(fixture.notebook(), firstReport), "Asia/Shanghai");

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
