package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SpacedRepetitionCorrectRecallMaximumIntervalSchedulingTest
    extends SpacedRepetitionRecallSchedulingTestBase {
  static final float MAXIMUM_INTERVAL_HOURS = 876000f;
  static final float LEGACY_LADDER_MAX_STABILITY_HOURS = 1_800_600f;

  @Test
  void correctRecallFromOverCapStabilityLandsAtTheCap() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(LEGACY_LADDER_MAX_STABILITY_HOURS);
    Timestamp gradeTime = onTimeGradeTime(memoryTracker);

    memoryTracker.recalledSuccessfully(gradeTime, null);

    assertThat(memoryTracker.getStability(), equalTo(MAXIMUM_INTERVAL_HOURS));
    assertThat(
        memoryTracker.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                gradeTime, Math.round(MAXIMUM_INTERVAL_HOURS))));
  }

  @Test
  void thinkingTimeOnOverCapCorrectRecallDoesNotPierceTheCap() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(LEGACY_LADDER_MAX_STABILITY_HOURS);

    memoryTracker.recalledSuccessfully(onTimeGradeTime(memoryTracker), 0);

    assertThat(memoryTracker.getStability(), equalTo(MAXIMUM_INTERVAL_HOURS));
  }
}
