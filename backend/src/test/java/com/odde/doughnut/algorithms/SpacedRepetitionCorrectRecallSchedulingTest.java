package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_DIFFICULTY;
import static com.odde.doughnut.entities.ForgettingCurve.FIRST_SUCCESS_STABILITY_HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SpacedRepetitionCorrectRecallSchedulingTest extends SpacedRepetitionRecallSchedulingTestBase {
  @Test
  void firstCorrectRecallInitializesDifficulty() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();

    memoryTracker.recalledSuccessfully(memoryTracker.getNextRecallAt(), null);

    assertThat(memoryTracker.getDifficulty(), equalTo(DEFAULT_DIFFICULTY));
    assertThat(memoryTracker.getStability(), equalTo(FIRST_SUCCESS_STABILITY_HOURS));
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

    unsetDifficulty.recalledSuccessfully(gradeTime, null);
    difficultyFive.recalledSuccessfully(gradeTime, null);

    assertThat(unsetDifficulty.getDifficulty(), equalTo(difficultyFive.getDifficulty()));
    assertThat(unsetDifficulty.getStability(), equalTo(difficultyFive.getStability()));
  }

  @Test
  void sameHourCorrectRecallGrowsFirstIntervalStabilityToTwentyFive() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(FIRST_SUCCESS_STABILITY_HOURS);

    memoryTracker.recalledSuccessfully(sameHourGradeTime(memoryTracker), null);

    assertThat(memoryTracker.getStability(), equalTo(25.0f));
  }

  @Test
  void sameHourEasyRecallGrowsFirstIntervalStabilityToFortyThree() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(FIRST_SUCCESS_STABILITY_HOURS);

    memoryTracker.recalledEasily(sameHourGradeTime(memoryTracker));

    assertThat(memoryTracker.getStability(), equalTo(43.0f));
  }

  @Test
  void sameHourHardRecallDoesNotShrinkFirstIntervalStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(FIRST_SUCCESS_STABILITY_HOURS);

    memoryTracker.recalledHard(sameHourGradeTime(memoryTracker));

    assertThat(memoryTracker.getStability(), equalTo(24.0f));
  }

  @Test
  void onTimeCorrectRecallUsesFsrsGoodStabilityIncrement() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.recalledSuccessfully(onTimeGradeTime(memoryTracker), null);

    assertThat(memoryTracker.getStability(), equalTo(266.0f));
  }

  @Test
  void onTimeCorrectRecallUpdatesDifficultyWithFsrsGoodNextD() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability(8f);

    memoryTracker.recalledSuccessfully(onTimeGradeTime(memoryTracker), null);

    assertThat(memoryTracker.getDifficulty(), equalTo(7.9872284f));
  }

  @Test
  void harderDifficultyGrowsStabilityLessOnCorrectRecall() {
    MemoryTracker easier = aGradedTrackerAtThreeDayStability(3f);
    MemoryTracker harder = aGradedTrackerAtThreeDayStability(8f);
    Timestamp gradeTime = onTimeGradeTime(easier);

    easier.recalledSuccessfully(gradeTime, null);
    harder.recalledSuccessfully(gradeTime, null);

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

    earlierProjection.recalledSuccessfully(gradeTime, null);
    laterProjection.recalledSuccessfully(gradeTime, null);

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

    wholeHourRecall.recalledSuccessfully(wholeHourGradeTime, null);
    recallWithSubHourRemainder.recalledSuccessfully(gradeTimeWithSubHourRemainder, null);

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
    earlierCorrect.markAsRecalled(failureTime, false, null);
    laterCorrect.markAsRecalled(failureTime, false, null);

    earlierCorrect.markAsRecalled(
        TimestampOperations.addHoursToTimestamp(failureTime, 12), true, null);
    laterCorrect.markAsRecalled(
        TimestampOperations.addHoursToTimestamp(failureTime, 24), true, null);

    assertThat(laterCorrect.getStability(), greaterThan(earlierCorrect.getStability()));
  }

  @Test
  void nextRecallAtIsLastRecalledAtPlusStabilityHours() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.recalledSuccessfully(gradeTime, null);

    long interval = TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), gradeTime);
    assertThat(interval, equalTo((long) Math.round(memoryTracker.getStability())));
  }

  @Test
  void overdueCorrectRecallLengthensStabilityMoreThanOnTime() {
    MemoryTracker onTime = aGradedTrackerAtThreeDayStability();
    MemoryTracker overdue = aGradedTrackerAtThreeDayStability();
    onTime.recalledSuccessfully(onTimeGradeTime(onTime), null);
    overdue.recalledSuccessfully(overdueGradeTime(overdue), null);

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
    elapsedTenTimesStability.recalledSuccessfully(
        TimestampOperations.addHoursToTimestamp(
            elapsedTenTimesStability.getLastRecalledAt(), stabilityHours * 10),
        null);
    elapsedHundredTimesStability.recalledSuccessfully(
        TimestampOperations.addHoursToTimestamp(
            elapsedHundredTimesStability.getLastRecalledAt(), stabilityHours * 100),
        null);

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
}
