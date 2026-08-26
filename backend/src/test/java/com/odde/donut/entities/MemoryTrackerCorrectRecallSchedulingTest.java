package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class MemoryTrackerCorrectRecallSchedulingTest extends MemoryTrackerRecallSchedulingTestBase {
  static final float FIRST_GOOD_DIFFICULTY = 2.118104f;
  static final float FIRST_GOOD_STABILITY_HOURS = 55.0f;
  static final float MAXIMUM_INTERVAL_HOURS = 876000f;
  static final float LEGACY_LADDER_MAX_STABILITY_HOURS = 1_800_600f;

  @Test
  void firstCorrectRecallUsesS0AndD0Good() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    Timestamp gradeTime = memoryTracker.getNextRecallAt();

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    assertThat(memoryTracker.getDifficulty(), equalTo(FIRST_GOOD_DIFFICULTY));
    assertThat(memoryTracker.getStability(), equalTo(FIRST_GOOD_STABILITY_HOURS));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                gradeTime, Math.round(FIRST_GOOD_STABILITY_HOURS))));
  }

  @Test
  void onTimeCorrectRecallAfterNewAgainUsesLongTermGoodStability() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    memoryTracker.applyGrade(memoryTracker.getNextRecallAt(), Grade.AGAIN);

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.GOOD);

    assertThat(memoryTracker.getStability(), equalTo(21.0f));
  }

  @Test
  void repeatedOnTimeCorrectRecallAfterNewAgainGrowsPastOneDay() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    memoryTracker.applyGrade(memoryTracker.getNextRecallAt(), Grade.AGAIN);

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.GOOD);
    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.GOOD);

    assertThat(memoryTracker.getStability(), equalTo(74.0f));
  }

  @Test
  void firstCorrectRecallIgnoresElapsedHours() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    Timestamp gradeTime =
        TimestampOperations.addHoursToTimestamp(memoryTracker.getAssimilatedAt(), 500);

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    assertThat(memoryTracker.getDifficulty(), equalTo(FIRST_GOOD_DIFFICULTY));
    assertThat(memoryTracker.getStability(), equalTo(FIRST_GOOD_STABILITY_HOURS));
  }

  @Test
  void correctRecallFillsUnsetDifficultyOnGradedTracker() {
    MemoryTracker unsetDifficulty =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .stabilityAndNextRecallAt(STABILITY_HOURS)
            .inMemoryPlease();
    MemoryTracker difficultyFive = aGradedTrackerAtThreeDayStability();
    Timestamp gradeTime = onTimeGradeTime(unsetDifficulty);

    unsetDifficulty.applyGrade(gradeTime, Grade.GOOD);
    difficultyFive.applyGrade(gradeTime, Grade.GOOD);

    assertThat(unsetDifficulty.getDifficulty(), equalTo(difficultyFive.getDifficulty()));
    assertThat(unsetDifficulty.getStability(), equalTo(difficultyFive.getStability()));
  }

  @Test
  void onTimeCorrectRecallUsesFsrsGoodStabilityIncrement() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.GOOD);

    assertThat(memoryTracker.getStability(), equalTo(266.0f));
  }

  @Test
  void onTimeCorrectRecallUpdatesDifficultyWithFsrsGoodNextD() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability(8f);

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.GOOD);

    assertThat(memoryTracker.getDifficulty(), equalTo(7.9872284f));
  }

  @Test
  void harderDifficultyGrowsStabilityLessOnCorrectRecall() {
    MemoryTracker easier = aGradedTrackerAtThreeDayStability(3f);
    MemoryTracker harder = aGradedTrackerAtThreeDayStability(8f);
    Timestamp gradeTime = onTimeGradeTime(easier);

    easier.applyGrade(gradeTime, Grade.GOOD);
    harder.applyGrade(gradeTime, Grade.GOOD);

    assertThat(harder.getStability(), lessThan(easier.getStability()));
  }

  @Test
  void correctRecallIntervalIsIndependentOfPersistedDueProjection() {
    MemoryTracker earlierProjection = aGradedTrackerAtThreeDayStability();
    MemoryTracker laterProjection = aGradedTrackerAtThreeDayStability();
    Timestamp gradeTime =
        TimestampOperations.addHoursToTimestamp(earlierProjection.getLastRecalledAt(), 24);
    earlierProjection.setNextRecallAt(TimestampOperations.addHoursToTimestamp(gradeTime, -24));
    laterProjection.setNextRecallAt(TimestampOperations.addHoursToTimestamp(gradeTime, 48));

    earlierProjection.applyGrade(gradeTime, Grade.GOOD);
    laterProjection.applyGrade(gradeTime, Grade.GOOD);

    long earlierInterval =
        TimestampOperations.getDiffInHours(earlierProjection.getNextRecallAt(), gradeTime);
    long laterInterval =
        TimestampOperations.getDiffInHours(laterProjection.getNextRecallAt(), gradeTime);
    assertThat(earlierInterval, equalTo(laterInterval));
  }

  @Test
  void correctRecallIntervalUsesWholeElapsedHours() {
    MemoryTracker wholeHourRecall = aGradedTrackerAtThreeDayStability();
    MemoryTracker recallWithSubHourRemainder = aGradedTrackerAtThreeDayStability();
    Timestamp wholeHourGradeTime =
        TimestampOperations.addHoursToTimestamp(wholeHourRecall.getLastRecalledAt(), 300);
    Timestamp gradeTimeWithSubHourRemainder =
        Timestamp.from(wholeHourGradeTime.toInstant().plusSeconds(30 * 60));

    wholeHourRecall.applyGrade(wholeHourGradeTime, Grade.GOOD);
    recallWithSubHourRemainder.applyGrade(gradeTimeWithSubHourRemainder, Grade.GOOD);

    long wholeHourInterval =
        TimestampOperations.getDiffInHours(wholeHourRecall.getNextRecallAt(), wholeHourGradeTime);
    long subHourRemainderInterval =
        TimestampOperations.getDiffInHours(
            recallWithSubHourRemainder.getNextRecallAt(), gradeTimeWithSubHourRemainder);
    assertThat(subHourRemainderInterval, equalTo(wholeHourInterval));
    assertThat(wholeHourRecall.getLastRecalledAt(), equalTo(wholeHourGradeTime));
    assertThat(
        recallWithSubHourRemainder.getLastRecalledAt(), equalTo(gradeTimeWithSubHourRemainder));
  }

  @Test
  void correctRecallAfterFailureUsesElapsedHoursSinceFailure() {
    MemoryTracker earlierCorrect = aGradedTrackerAtThreeDayStability();
    MemoryTracker laterCorrect = aGradedTrackerAtThreeDayStability();
    Timestamp failureTime =
        TimestampOperations.addHoursToTimestamp(earlierCorrect.getLastRecalledAt(), 300);
    earlierCorrect.applyGrade(failureTime, Grade.AGAIN);
    laterCorrect.applyGrade(failureTime, Grade.AGAIN);

    earlierCorrect.applyGrade(TimestampOperations.addHoursToTimestamp(failureTime, 24), Grade.GOOD);
    laterCorrect.applyGrade(TimestampOperations.addHoursToTimestamp(failureTime, 48), Grade.GOOD);

    assertThat(laterCorrect.getStability(), greaterThan(earlierCorrect.getStability()));
  }

  @Test
  void nextRecallAtIsLastRecalledAtPlusStabilityHours() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    long interval = TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), gradeTime);
    assertThat(interval, equalTo((long) Math.round(memoryTracker.getStability())));
  }

  @Test
  void overdueCorrectRecallLengthensStabilityMoreThanOnTime() {
    MemoryTracker onTime = aGradedTrackerAtThreeDayStability();
    MemoryTracker overdue = aGradedTrackerAtThreeDayStability();
    onTime.applyGrade(onTimeGradeTime(onTime), Grade.GOOD);
    overdue.applyGrade(overdueGradeTime(overdue), Grade.GOOD);

    long onTimeInterval =
        TimestampOperations.getDiffInHours(onTime.getNextRecallAt(), onTime.getLastRecalledAt());
    long overdueInterval =
        TimestampOperations.getDiffInHours(overdue.getNextRecallAt(), overdue.getLastRecalledAt());
    assertThat(overdueInterval, greaterThan(onTimeInterval));
    assertThat(overdue.getStability(), greaterThan(onTime.getStability()));
  }

  @Test
  void overdueCorrectRecallExtraGrowthConverges() {
    MemoryTracker elapsedTenTimesStability = aGradedTrackerAtThreeDayStability();
    MemoryTracker elapsedHundredTimesStability = aGradedTrackerAtThreeDayStability();
    int stabilityHours = Math.round(elapsedTenTimesStability.getStability());
    elapsedTenTimesStability.applyGrade(
        TimestampOperations.addHoursToTimestamp(
            elapsedTenTimesStability.getLastRecalledAt(), stabilityHours * 10),
        Grade.GOOD);
    elapsedHundredTimesStability.applyGrade(
        TimestampOperations.addHoursToTimestamp(
            elapsedHundredTimesStability.getLastRecalledAt(), stabilityHours * 100),
        Grade.GOOD);

    long tenTimesInterval =
        TimestampOperations.getDiffInHours(
            elapsedTenTimesStability.getNextRecallAt(),
            elapsedTenTimesStability.getLastRecalledAt());
    long hundredTimesInterval =
        TimestampOperations.getDiffInHours(
            elapsedHundredTimesStability.getNextRecallAt(),
            elapsedHundredTimesStability.getLastRecalledAt());
    assertThat(hundredTimesInterval, greaterThan(tenTimesInterval));
    assertThat(hundredTimesInterval - tenTimesInterval, lessThan(tenTimesInterval));
  }

  @Test
  void correctRecallFromOverCapStabilityLandsAtTheCap() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(LEGACY_LADDER_MAX_STABILITY_HOURS);
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    assertThat(memoryTracker.getStability(), equalTo(MAXIMUM_INTERVAL_HOURS));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                gradeTime, Math.round(MAXIMUM_INTERVAL_HOURS))));
  }
}
