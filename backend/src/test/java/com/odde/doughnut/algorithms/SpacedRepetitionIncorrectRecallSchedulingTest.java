package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SpacedRepetitionIncorrectRecallSchedulingTest
    extends SpacedRepetitionRecallSchedulingTestBase {
  @Test
  void onTimeIncorrectRecallUsesFsrsAgainPostLapseStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();
    Integer oldRecallCount = memoryTracker.getRecallCount();
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.markAsRecalled(gradeTime, false, null);

    assertThat(memoryTracker.getStability(), equalTo(17.0f));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, 12)));
    assertThat(memoryTracker.getLastRecalledAt(), equalTo(gradeTime));
    assertThat(memoryTracker.getRecallCount(), equalTo(oldRecallCount + 1));
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
  void incorrectRecallFromOneHourStabilityPersistsAtLeastOneHour() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(1f);

    memoryTracker.markAsRecalled(onTimeGradeTime(memoryTracker), false, null);

    assertThat(memoryTracker.getStability(), greaterThanOrEqualTo(1f));
  }

  @Test
  void newTrackerIncorrectRecallKeepsZeroStabilityAndTwelveHourDue() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).inMemoryPlease();
    Timestamp gradeTime = memoryTracker.getNextRecallAt();

    memoryTracker.markAsRecalled(gradeTime, false, null);

    assertThat(memoryTracker.getStability(), equalTo(ASSIMILATE_STABILITY_HOURS));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(TimestampOperations.addHoursToTimestamp(gradeTime, 12)));
  }
}
