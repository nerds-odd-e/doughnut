package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_DIFFICULTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SpacedRepetitionRecallSchedulingTest {
  private static final float STABILITY_HOURS = 72f;
  private final MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  private final User user = makeMe.aUser().inMemoryPlease();
  private final Note note = makeMe.aNote().inMemoryPlease();

  private MemoryTracker aGradedTrackerAtThreeDayStability() {
    return aGradedTrackerAtThreeDayStability(DEFAULT_DIFFICULTY);
  }

  private MemoryTracker aGradedTrackerAtThreeDayStability(float difficulty) {
    return makeMe
        .aMemoryTrackerFor(note)
        .by(user)
        .stabilityAndNextRecallAt(STABILITY_HOURS)
        .difficulty(difficulty)
        .inMemoryPlease();
  }

  private Timestamp onTimeGradeTime(MemoryTracker tracker) {
    return TimestampOperations.addHoursToTimestamp(
        tracker.getLastRecalledAt(), Math.round(tracker.getStability()));
  }

  @Test
  void onTimeCorrectRecallUsesFsrsGoodStabilityIncrement() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.recalledSuccessfully(onTimeGradeTime(memoryTracker), null);

    assertThat(memoryTracker.getStability(), equalTo(266.0f));
  }

  @Test
  void onTimeCorrectRecallUpdatesDifficultyTowardEasyInit() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability(8f);

    memoryTracker.recalledSuccessfully(onTimeGradeTime(memoryTracker), null);

    assertThat(memoryTracker.getDifficulty(), equalTo(7.998413f));
  }

  @Test
  void harderDifficultyGrowsStabilityLessOnCorrectRecall() {
    MemoryTracker easier = aGradedTrackerAtThreeDayStability(3f);
    MemoryTracker harder = aGradedTrackerAtThreeDayStability(8f);
    Timestamp gradeTime = onTimeGradeTime(easier);

    easier.recalledSuccessfully(gradeTime, null);
    harder.recalledSuccessfully(gradeTime, null);

    assertThat(harder.getStability(), lessThan(easier.getStability()));
    assertThat(easier.getStability(), greaterThanOrEqualTo(STABILITY_HOURS));
    assertThat(harder.getStability(), greaterThanOrEqualTo(STABILITY_HOURS));
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
    overdue.recalledSuccessfully(
        TimestampOperations.addHoursToTimestamp(
            overdue.getLastRecalledAt(), Math.round(overdue.getStability()) * 2),
        null);

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
