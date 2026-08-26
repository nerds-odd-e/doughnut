package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class MemoryTrackerIncorrectRecallSchedulingTest extends MemoryTrackerRecallSchedulingTestBase {
  static final float FIRST_AGAIN_DIFFICULTY = 6.4133f;
  static final float FIRST_AGAIN_STABILITY_HOURS = 5f;

  @Test
  void onTimeIncorrectRecallUsesFsrsAgainPostLapseStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.AGAIN);

    assertThat(memoryTracker.getStability(), equalTo(17.0f));
  }

  @Test
  void yearOverdueIncorrectRecallOnFiveHourStabilityStaysAtFive() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(5f, 1f);

    memoryTracker.applyGrade(
        TimestampOperations.addHoursToTimestamp(memoryTracker.getLastRecalledAt(), 8760),
        Grade.AGAIN);

    assertThat(memoryTracker.getStability(), equalTo(5f));
  }

  @Test
  void overdueIncorrectRecallLeavesMoreRemainingStabilityThanOnTime() {
    MemoryTracker onTime = aGradedTrackerAtThreeDayStability();
    MemoryTracker overdue = aGradedTrackerAtThreeDayStability();
    onTime.applyGrade(onTimeGradeTime(onTime), Grade.AGAIN);
    overdue.applyGrade(overdueGradeTime(overdue), Grade.AGAIN);

    assertThat(overdue.getStability(), greaterThan(onTime.getStability()));
  }

  @Test
  void harderDifficultyLeavesLessRemainingStabilityOnIncorrectRecall() {
    MemoryTracker easier = aGradedTrackerAtThreeDayStability(3f);
    MemoryTracker harder = aGradedTrackerAtThreeDayStability(8f);
    Timestamp gradeTime = onTimeGradeTime(easier);

    easier.applyGrade(gradeTime, Grade.AGAIN);
    harder.applyGrade(gradeTime, Grade.AGAIN);

    assertThat(harder.getStability(), lessThan(easier.getStability()));
  }

  @Test
  void incorrectRecallFromOneHourStabilityPersistsOneHour() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(1f);

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.AGAIN);

    assertThat(memoryTracker.getStability(), equalTo(1f));
  }

  @Test
  void newTrackerIncorrectRecallUsesS0AndD0Again() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    Timestamp gradeTime = memoryTracker.getNextRecallAt();

    memoryTracker.applyGrade(gradeTime, Grade.AGAIN);

    assertThat(memoryTracker.getDifficulty(), equalTo(FIRST_AGAIN_DIFFICULTY));
    assertThat(memoryTracker.getStability(), equalTo(FIRST_AGAIN_STABILITY_HOURS));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                gradeTime, Math.round(FIRST_AGAIN_STABILITY_HOURS))));
  }

  @Test
  void onTimeIncorrectRecallAfterFirstGoodUsesFsrsAgainFromS0AndD0Good() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    memoryTracker.applyGrade(memoryTracker.getNextRecallAt(), Grade.GOOD);
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.applyGrade(gradeTime, Grade.AGAIN);

    assertThat(memoryTracker.getStability(), equalTo(15.0f));
    assertThat(memoryTracker.getDifficulty(), equalTo(7.3945026f));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, 15)));
  }

  @Test
  void onTimeIncorrectRecallUpdatesDifficultyWithFsrsAgainNextD() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.applyGrade(onTimeGradeTime(memoryTracker), Grade.AGAIN);

    assertThat(memoryTracker.getDifficulty(), equalTo(8.341763f));
  }

  @Test
  void incorrectRecallFillsUnsetDifficultyOnGradedTracker() {
    MemoryTracker unsetDifficulty =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .stabilityAndNextRecallAt(STABILITY_HOURS)
            .inMemoryPlease();
    MemoryTracker difficultyFive = aGradedTrackerAtThreeDayStability();
    Timestamp gradeTime = onTimeGradeTime(unsetDifficulty);

    unsetDifficulty.applyGrade(gradeTime, Grade.AGAIN);
    difficultyFive.applyGrade(gradeTime, Grade.AGAIN);

    assertThat(unsetDifficulty.getDifficulty(), equalTo(difficultyFive.getDifficulty()));
  }
}
