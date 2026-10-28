package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MemoryTrackerCorrectRecallDueFuzzTest extends MemoryTrackerRecallSchedulingTestBase {
  static final float FIRST_EASY_STABILITY_HOURS = 199.0f;
  static final float MAXIMUM_INTERVAL_HOURS = 876000f;
  static final float LEGACY_LADDER_MAX_STABILITY_HOURS = 1_800_600f;

  @ParameterizedTest
  @CsvSource({"GOOD, 55", "HARD, 31"})
  void firstCorrectRecallBelowFuzzThresholdUsesUnfuzzedStabilityInterval(
      Grade grade, float stabilityHours) {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    Timestamp gradeTime = memoryTracker.getNextRecallAt();

    memoryTracker.applyGrade(gradeTime, grade);

    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, Math.round(stabilityHours))));
  }

  @Test
  void nextRecallAtBelowFuzzThresholdIsLastRecalledAtPlusStabilityHours() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    memoryTracker.applyGrade(memoryTracker.getNextRecallAt(), Grade.AGAIN);
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    long interval = TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), gradeTime);
    assertThat(interval, equalTo(21L));
  }

  @Test
  void nextRecallAtAtOrAboveFuzzThresholdUsesBoundedHourFuzzWithoutChangingStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();
    memoryTracker.id = 17;
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    float stability = memoryTracker.getStability();
    assertThat(stability, equalTo(266.0f));
    long interval = TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), gradeTime);
    int max = Math.round(MAXIMUM_INTERVAL_HOURS);
    int expected = fuzzedHours(17, gradeTime, stability, max);
    int[] bounds = MemoryTrackerRecallIntervalFuzz.constrainedFuzzBounds(stability, 1, max);
    assertThat(interval, equalTo((long) expected));
    assertThat(interval, greaterThanOrEqualTo((long) bounds[0]));
    assertThat(interval, lessThanOrEqualTo((long) bounds[1]));
    assertThat(memoryTracker.getNextRecallAt().after(gradeTime), equalTo(true));
  }

  @Test
  void matchingMemoryStatesWithDifferentTrackerIdsCanReceiveDifferentFuzzedIntervals() {
    MemoryTracker probe = aGradedTrackerAtThreeDayStability();
    Timestamp gradeTime = onTimeGradeTime(probe);
    probe.applyGrade(gradeTime, Grade.GOOD);
    float stability = probe.getStability();
    int max = Math.round(MAXIMUM_INTERVAL_HOURS);
    int leftId = 1;
    int rightId = 2;
    while (fuzzedHours(leftId, gradeTime, stability, max)
        == fuzzedHours(rightId, gradeTime, stability, max)) {
      rightId++;
    }
    MemoryTracker first = aGradedTrackerAtThreeDayStability();
    MemoryTracker second = aGradedTrackerAtThreeDayStability();
    first.id = leftId;
    second.id = rightId;

    first.applyGrade(gradeTime, Grade.GOOD);
    second.applyGrade(gradeTime, Grade.GOOD);

    assertThat(first.getStability(), equalTo(second.getStability()));
    assertThat(first.getDifficulty(), equalTo(second.getDifficulty()));
    long firstInterval = TimestampOperations.getDiffInHours(first.getNextRecallAt(), gradeTime);
    long secondInterval = TimestampOperations.getDiffInHours(second.getNextRecallAt(), gradeTime);
    assertThat(firstInterval, equalTo((long) fuzzedHours(leftId, gradeTime, stability, max)));
    assertThat(secondInterval, equalTo((long) fuzzedHours(rightId, gradeTime, stability, max)));
    assertThat(firstInterval, not(equalTo(secondInterval)));
  }

  @Test
  void firstEasyRecallUsesFuzzedIntervalWithinBounds() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    memoryTracker.id = 9;
    Timestamp gradeTime = memoryTracker.getNextRecallAt();

    memoryTracker.applyGrade(gradeTime, Grade.EASY);

    long interval = TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), gradeTime);
    assertThat(
        interval,
        equalTo(
            (long)
                fuzzedHours(
                    9, gradeTime, FIRST_EASY_STABILITY_HOURS, Math.round(MAXIMUM_INTERVAL_HOURS))));
  }

  @Test
  void correctRecallFromOverCapStabilityLandsAtTheCap() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(LEGACY_LADDER_MAX_STABILITY_HOURS);
    memoryTracker.id = 3;
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.applyGrade(gradeTime, Grade.GOOD);

    assertThat(memoryTracker.getStability(), equalTo(MAXIMUM_INTERVAL_HOURS));
    long interval = TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), gradeTime);
    int expected =
        fuzzedHours(3, gradeTime, MAXIMUM_INTERVAL_HOURS, Math.round(MAXIMUM_INTERVAL_HOURS));
    assertThat(interval, equalTo((long) expected));
    assertThat(interval, lessThanOrEqualTo((long) Math.round(MAXIMUM_INTERVAL_HOURS)));
    assertThat(memoryTracker.getNextRecallAt().after(gradeTime), equalTo(true));
  }

  private static int fuzzedHours(int trackerId, Timestamp gradeTime, float stability, int max) {
    return MemoryTrackerRecallIntervalFuzz.intervalHoursForGrade(
        trackerId, gradeTime, stability, 1, max);
  }
}
