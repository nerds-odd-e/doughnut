package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
  static final float FIRST_HARD_DIFFICULTY = 5.1121707f;
  static final float FIRST_HARD_STABILITY_HOURS = 31f;
  static final float FIRST_AGAIN_DIFFICULTY = 6.4133f;
  static final float FIRST_AGAIN_STABILITY_HOURS = 5f;

  @Test
  void firstScoreThreeOnNewPersistsD0Good() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    assertThat(fixture.holaTracker().getDifficulty(), nullValue());

    controller.record(recordRequest(fixture.notebook(), HOLA3_GRACIAS1_REPORT), "Asia/Shanghai");

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
  void firstScoreFourOnNewPersistsD0Easy() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstScore(4);
    assertThat(hola.getDifficulty(), equalTo(FIRST_EASY_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_EASY_STABILITY_HOURS));
  }

  @Test
  void firstScoreTwoOnNewPersistsD0Hard() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstScore(2);
    assertThat(hola.getDifficulty(), equalTo(FIRST_HARD_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_HARD_STABILITY_HOURS));
  }

  @ParameterizedTest
  @CsvSource({"4, 484", "3, 284", "2, 193"})
  void onTimeSecondScorePersistsStability(int score, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterOnTimeSecondScore(score).holaTracker().getStability(), equalTo(expectedStability));
  }

  @Test
  void onTimeSecondScoreThreePersistsGoodNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(3).holaTracker().getDifficulty(), equalTo(2.1112142f));
  }

  @Test
  void onTimeSecondScoreFourPersistsEasyNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(4).holaTracker().getDifficulty(), equalTo(1f));
  }

  @Test
  void onTimeSecondScoreTwoPersistsHardNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondScore(2).holaTracker().getDifficulty(), equalTo(4.7528586f));
  }

  @ParameterizedTest
  @CsvSource({"3, 416", "4, 731", "2, 272"})
  void overdueSecondScorePersistsStability(int score, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterStaggeredSecondScore(score).graciasTracker().getStability(),
        equalTo(expectedStability));
  }

  @Test
  void overdueSecondScoreThreeGrowsStabilityMoreThanOnTime()
      throws UnexpectedNoAccessRightException {
    SpanishNotebookFixture fixture = afterStaggeredSecondScore(3);
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
  void firstScoreOneOnNewPersistsD0Again() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstScore(1);
    assertThat(hola.getDifficulty(), equalTo(FIRST_AGAIN_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_AGAIN_STABILITY_HOURS));
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
    Timestamp firstRecord = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(firstRecord);

    SpanishNotebookFixture fixture = spanishNotebookFixture(firstRecord);
    controller.record(recordRequest(fixture.notebook(), HOLA3_GRACIAS1_REPORT), "Asia/Shanghai");

    Timestamp secondRecord =
        TimestampOperations.addHoursToTimestamp(
            firstRecord, Math.round(FIRST_GOOD_STABILITY_HOURS));
    testabilitySettings.timeTravelTo(secondRecord);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", score)), "Asia/Shanghai");

    return fixture;
  }

  private SpanishNotebookFixture afterStaggeredSecondScore(int score)
      throws UnexpectedNoAccessRightException {
    Timestamp firstRecord = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(firstRecord);

    SpanishNotebookFixture fixture = spanishNotebookFixture(firstRecord);
    controller.record(recordRequest(fixture.notebook(), HOLA3_GRACIAS3_REPORT), "Asia/Shanghai");

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
