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
  void firstGradeThreeOnNewPersistsD0Good() throws UnexpectedNoAccessRightException {
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
  void firstGradeFourOnNewPersistsD0Easy() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstGrade(4);
    assertThat(hola.getDifficulty(), equalTo(FIRST_EASY_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_EASY_STABILITY_HOURS));
  }

  @Test
  void firstGradeTwoOnNewPersistsD0Hard() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstGrade(2);
    assertThat(hola.getDifficulty(), equalTo(FIRST_HARD_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_HARD_STABILITY_HOURS));
  }

  @ParameterizedTest
  @CsvSource({"4, 484", "3, 284", "2, 193"})
  void onTimeSecondGradePersistsStability(int grade, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterOnTimeSecondGrade(grade).holaTracker().getStability(), equalTo(expectedStability));
  }

  @Test
  void onTimeSecondGradeThreePersistsGoodNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondGrade(3).holaTracker().getDifficulty(), equalTo(2.1112142f));
  }

  @Test
  void onTimeSecondGradeFourPersistsEasyNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondGrade(4).holaTracker().getDifficulty(), equalTo(1f));
  }

  @Test
  void onTimeSecondGradeTwoPersistsHardNextDifficulty() throws UnexpectedNoAccessRightException {
    assertThat(afterOnTimeSecondGrade(2).holaTracker().getDifficulty(), equalTo(4.7528586f));
  }

  @ParameterizedTest
  @CsvSource({"3, 416", "4, 731", "2, 272"})
  void overdueSecondGradePersistsStability(int grade, float expectedStability)
      throws UnexpectedNoAccessRightException {
    assertThat(
        afterStaggeredSecondGrade(grade).graciasTracker().getStability(),
        equalTo(expectedStability));
  }

  @Test
  void overdueSecondGradeThreeGrowsStabilityMoreThanOnTime()
      throws UnexpectedNoAccessRightException {
    SpanishNotebookFixture fixture = afterStaggeredSecondGrade(3);
    assertThat(
        fixture.graciasTracker().getStability(), greaterThan(fixture.holaTracker().getStability()));
  }

  @Test
  void onTimeSecondGradeOnePersistsAgainSchedule() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = afterOnTimeSecondGrade(1).holaTracker();
    assertThat(hola.getStability(), equalTo(15f));
    assertThat(hola.getDifficulty(), equalTo(7.3945026f));
    assertThat(
        hola.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                hola.getLastRecalledAt(), Math.round(hola.getStability()))));
  }

  @Test
  void firstGradeOneOnNewPersistsD0Again() throws UnexpectedNoAccessRightException {
    MemoryTracker hola = holaAfterFirstGrade(1);
    assertThat(hola.getDifficulty(), equalTo(FIRST_AGAIN_DIFFICULTY));
    assertThat(hola.getStability(), equalTo(FIRST_AGAIN_STABILITY_HOURS));
  }

  private MemoryTracker holaAfterFirstGrade(int grade) throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);
    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", grade)), "Asia/Shanghai");
    return fixture.holaTracker();
  }

  private SpanishNotebookFixture afterOnTimeSecondGrade(int grade)
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
        recordRequest(fixture.notebook(), learningSessionReport("Hola", grade)), "Asia/Shanghai");

    return fixture;
  }

  private SpanishNotebookFixture afterStaggeredSecondGrade(int grade)
      throws UnexpectedNoAccessRightException {
    Timestamp firstRecord = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(firstRecord);

    SpanishNotebookFixture fixture = spanishNotebookFixture(firstRecord);
    controller.record(recordRequest(fixture.notebook(), HOLA3_GRACIAS3_REPORT), "Asia/Shanghai");

    testabilitySettings.timeTravelTo(
        TimestampOperations.addHoursToTimestamp(
            firstRecord, Math.round(FIRST_GOOD_STABILITY_HOURS)));
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", grade)), "Asia/Shanghai");

    testabilitySettings.timeTravelTo(
        TimestampOperations.addHoursToTimestamp(
            firstRecord, Math.round(FIRST_GOOD_STABILITY_HOURS) * 2));
    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Gracias", grade)),
        "Asia/Shanghai");

    return fixture;
  }
}
