package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.utils.TimestampOperations;
import org.junit.jupiter.api.Test;

class SpacedRepetitionEarlyRecallAdjustmentTest extends SpacedRepetitionRecallSchedulingTestBase {
  @Test
  void earlyLongTermCorrectGrowsLessThanOnTime() {
    float early = nextStabilityHours(24);
    float onTime = nextStabilityHours(Math.round(STABILITY_HOURS));
    assertThat(early, greaterThanOrEqualTo(STABILITY_HOURS));
    assertThat(early, lessThan(onTime));
  }

  @Test
  void almostOnTimeEarlyCorrectGrowsLessThanOrEqualToOnTime() {
    float almostOnTime = nextStabilityHours(Math.round(STABILITY_HOURS) - 1);
    float onTime = nextStabilityHours(Math.round(STABILITY_HOURS));
    assertThat(almostOnTime, greaterThan(STABILITY_HOURS));
    assertThat(almostOnTime, lessThanOrEqualTo(onTime));
  }

  private float nextStabilityHours(int elapsedInHours) {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();
    memoryTracker.recalledSuccessfully(
        TimestampOperations.addHoursToTimestamp(memoryTracker.getLastRecalledAt(), elapsedInHours));
    return memoryTracker.getStability();
  }
}
