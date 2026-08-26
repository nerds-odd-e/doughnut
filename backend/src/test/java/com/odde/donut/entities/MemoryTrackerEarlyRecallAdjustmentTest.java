package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.junit.jupiter.api.Test;

class MemoryTrackerEarlyRecallAdjustmentTest extends MemoryTrackerRecallSchedulingTestBase {
  @Test
  void earlyLongTermCorrectGrowsLessThanOnTime() {
    float early = nextStabilityHours(24);
    float onTime = nextStabilityHours(Math.round(STABILITY_HOURS));
    assertThat(early, lessThan(onTime));
  }

  @Test
  void almostOnTimeEarlyCorrectGrowsLessThanOrEqualToOnTime() {
    float almostOnTime = nextStabilityHours(Math.round(STABILITY_HOURS) - 1);
    float onTime = nextStabilityHours(Math.round(STABILITY_HOURS));
    assertThat(almostOnTime, greaterThan(STABILITY_HOURS));
    assertThat(almostOnTime, lessThanOrEqualTo(onTime));
  }
}
