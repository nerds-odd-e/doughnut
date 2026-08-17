package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.MemoryTracker;
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

  @ParameterizedTest
  @CsvSource({"4, 102", "5, 169", "3, 71"})
  void onTimeSecondScorePersistsStability(int score, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterOnTimeSecondScore(score).holaTracker().getStability(), equalTo(expectedStability));
  }

  @Test
  void onTimeSecondScoreFourPersistsGoodNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(4).holaTracker().getDifficulty(), equalTo(4.990228f));
  }

  @Test
  void onTimeSecondScoreFivePersistsEasyNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(5).holaTracker().getDifficulty(), equalTo(3.3144615f));
  }

  @Test
  void onTimeSecondScoreThreePersistsHardNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(3).holaTracker().getDifficulty(), equalTo(6.6659956f));
  }

  @ParameterizedTest
  @CsvSource({"4, 146", "5, 253", "3, 97"})
  void overdueSecondScorePersistsStability(int score, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterStaggeredSecondScore(score).graciasTracker().getStability(),
        equalTo(expectedStability));
  }

  @Test
  void overdueSecondScoreFourGrowsStabilityMoreThanOnTime()
      throws UnexpectedNoAccessRightException {
    SpanishNotebookFixture fixture = afterStaggeredSecondScore(4);
    assertThat(
        fixture.graciasTracker().getStability(), greaterThan(fixture.holaTracker().getStability()));
  }

  @Test
  void onTimeSecondScoreOnePersistsAgainSchedule() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = afterOnTimeSecondScore(1).holaTracker();
    assertThat(hola.getStability(), equalTo(8f));
    assertThat(hola.getDifficulty(), equalTo(8.341763f));
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
    MemoryTracker hola = afterOnTimeSecondScore(2).holaTracker();
    assertThat(hola.getStability(), equalTo(19f));
    assertThat(hola.getDifficulty(), equalTo(5f));
  }

  @ParameterizedTest
  @CsvSource({"0", "1", "2"})
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

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(fixture.notebook(), learningSessionReport("Hola", 0)), "Asia/Shanghai");

    assertTrue(fixture.holaTracker().getNextRecallAt().after(response.getRecordedAt()));
  }

  private SpanishNotebookFixture afterOnTimeSecondScore(int score)
      throws UnexpectedNoAccessRightException {
    return afterOnTimeSecondScore(HOLA4_GRACIAS1_REPORT, learningSessionReport("Hola", score));
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
