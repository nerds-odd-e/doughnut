package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SpacedRepetitionIncorrectRecallSchedulingTest
    extends SpacedRepetitionRecallSchedulingTestBase {
  @Test
  void onTimeIncorrectRecallUsesFsrsAgainPostLapseStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.markAsRecalled(onTimeGradeTime(memoryTracker), false, null);

    assertThat(memoryTracker.getStability(), equalTo(17.0f));
  }

  @Test
  void overdueIncorrectRecallLeavesMoreRemainingStabilityThanOnTime() {
    MemoryTracker onTime = aGradedTrackerAtThreeDayStability();
    MemoryTracker overdue = aGradedTrackerAtThreeDayStability();
    onTime.markAsRecalled(onTimeGradeTime(onTime), false, null);
    overdue.markAsRecalled(overdueGradeTime(overdue), false, null);

    assertThat(overdue.getStability(), greaterThan(onTime.getStability()));
  }

  @Test
  void harderDifficultyLeavesLessRemainingStabilityOnIncorrectRecall() {
    MemoryTracker easier = aGradedTrackerAtThreeDayStability(3f);
    MemoryTracker harder = aGradedTrackerAtThreeDayStability(8f);
    Timestamp gradeTime = onTimeGradeTime(easier);

    easier.markAsRecalled(gradeTime, false, null);
    harder.markAsRecalled(gradeTime, false, null);

    assertThat(harder.getStability(), lessThan(easier.getStability()));
  }

  @Test
  void incorrectRecallFromOneHourStabilityPersistsOneHour() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(1f);

    memoryTracker.markAsRecalled(onTimeGradeTime(memoryTracker), false, null);

    assertThat(memoryTracker.getStability(), equalTo(1f));
  }

  @Test
  void newTrackerIncorrectRecallKeepsZeroStabilityAndTwentyFourHourDue() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    Timestamp gradeTime = memoryTracker.getNextRecallAt();

    memoryTracker.markAsRecalled(gradeTime, false, null);

    assertThat(memoryTracker.getStability(), equalTo(ASSIMILATE_STABILITY_HOURS));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, 24)));
    assertThat(memoryTracker.getDifficulty(), nullValue());
  }

  @Test
  void onTimeIncorrectRecallAfterFirstGoodUsesFsrsAgainFromS0AndD0Good() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    memoryTracker.recalledSuccessfully(memoryTracker.getNextRecallAt(), null);
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.markAsRecalled(gradeTime, false, null);

    assertThat(memoryTracker.getStability(), equalTo(15.0f));
    assertThat(memoryTracker.getDifficulty(), equalTo(7.3945026f));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, 15)));
  }

  @Test
  void onTimeIncorrectRecallUpdatesDifficultyWithFsrsAgainNextD() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.markAsRecalled(onTimeGradeTime(memoryTracker), false, null);

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

    unsetDifficulty.markAsRecalled(gradeTime, false, null);
    difficultyFive.markAsRecalled(gradeTime, false, null);

    assertThat(unsetDifficulty.getDifficulty(), equalTo(difficultyFive.getDifficulty()));
  }
}
