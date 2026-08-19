package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;

class MemoryTrackerSameHourRecallSchedulingTest extends MemoryTrackerRecallSchedulingTestBase {
  @Test
  void sameHourCorrectRecallGrowsFirstIntervalStabilityToTwentyFive() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(24f);

    memoryTracker.recalledSuccessfully(sameHourGradeTime(memoryTracker));

    assertThat(memoryTracker.getStability(), equalTo(25.0f));
  }

  @Test
  void sameHourEasyRecallGrowsFirstIntervalStabilityToFortyThree() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(24f);

    memoryTracker.recalledEasily(sameHourGradeTime(memoryTracker));

    assertThat(memoryTracker.getStability(), equalTo(43.0f));
  }

  @Test
  void sameHourHardRecallDoesNotShrinkFirstIntervalStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(24f);

    memoryTracker.recalledHard(sameHourGradeTime(memoryTracker));

    assertThat(memoryTracker.getStability(), equalTo(24.0f));
  }

  @Test
  void sameHourCorrectRecallDoesNotShrinkThreeDayStability() {
    MemoryTracker memoryTracker = aGradedTrackerAtThreeDayStability();

    memoryTracker.recalledSuccessfully(sameHourGradeTime(memoryTracker));

    assertThat(memoryTracker.getStability(), equalTo(STABILITY_HOURS));
  }

  @Test
  void twentyThreeHourCorrectRecallDoesNotShrinkThreeDayStability() {
    assertThat(nextStabilityHours(23), equalTo(STABILITY_HOURS));
  }

  @Test
  void twentyFourHourCorrectRecallGrowsThreeDayStability() {
    assertThat(nextStabilityHours(24), greaterThan(STABILITY_HOURS));
  }
}
