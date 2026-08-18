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
  static final float FIRST_GOOD_DIFFICULTY = 2.118104f;
  static final float FIRST_GOOD_STABILITY_HOURS = 55f;
  static final float FIRST_EASY_DIFFICULTY = 1f;
  static final float FIRST_EASY_STABILITY_HOURS = 199f;

  @Test
  void firstScoreFourOnNewPersistsD0Good() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    assertThat(fixture.holaTracker().getDifficulty(), nullValue());

    controller.record(recordRequest(fixture.notebook(), HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    MemoryTracker hola = fixture.holaTracker();
    assertThat(hola.getDifficulty(), equalTo(FIRST_GOOD_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_GOOD_STABILITY_HOURS));
    assertThat(
        hola.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                hola.getLastRecalledAt(), Math.round(FIRST_GOOD_STABILITY_HOURS))));
  }

  @Test
  void firstScoreFiveOnNewPersistsD0Easy() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstScore(5);
    assertThat(hola.getDifficulty(), equalTo(FIRST_EASY_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_EASY_STABILITY_HOURS));
  }

  @Test
  void firstScoreThreeOnNewPersistsDifficultyFiveAndStability24()
      throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstScore(3);
    assertThat(hola.getDifficulty(), equalTo(5f));
    assertThat(hola.getStability(), equalTo(24f));
  }

  @ParameterizedTest
  @CsvSource({"4, 284", "5, 484", "3, 193"})
  void onTimeSecondScorePersistsStability(int score, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterOnTimeSecondScore(score).holaTracker().getStability(), equalTo(expectedStability));
  }

  @Test
  void onTimeSecondScoreFourPersistsGoodNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(4).holaTracker().getDifficulty(), equalTo(2.1112142f));
  }

  @Test
  void onTimeSecondScoreFivePersistsEasyNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(5).holaTracker().getDifficulty(), equalTo(1f));
  }

  @Test
  void onTimeSecondScoreThreePersistsHardNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(3).holaTracker().getDifficulty(), equalTo(4.7528586f));
  }

  @ParameterizedTest
  @CsvSource({"4, 416", "5, 731", "3, 272"})
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
    assertThat(hola.getStability(), equalTo(15f));
    assertThat(hola.getDifficulty(), equalTo(7.3945026f));
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
    assertThat(hola.getStability(), equalTo(44f));
    assertThat(hola.getDifficulty(), equalTo(FIRST_GOOD_DIFFICULTY));
  }

  @ParameterizedTest
  @CsvSource({"0", "1", "2"})
  void firstScoreLeavesDifficultyUnsetAndStabilityZero(int score)
      throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstScore(score);
    assertThat(hola.getDifficulty(), nullValue());
    assertThat(hola.getStability(), equalTo(0f));
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

  private MemoryTracker holaAfterFirstScore(int score) throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);
    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");
    return fixture.holaTracker();
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

    Timestamp secondRecord =
        TimestampOperations.addHoursToTimestamp(
            firstRecord, Math.round(FIRST_GOOD_STABILITY_HOURS));
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

    testabilitySettings.timeTravelTo(
        TimestampOperations.addHoursToTimestamp(
            firstRecord, Math.round(FIRST_GOOD_STABILITY_HOURS)));
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");

    testabilitySettings.timeTravelTo(
        TimestampOperations.addHoursToTimestamp(
            firstRecord, Math.round(FIRST_GOOD_STABILITY_HOURS) * 2));
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Gracias", score)),
        "Asia/Shanghai");

    return fixture;
  }
}
