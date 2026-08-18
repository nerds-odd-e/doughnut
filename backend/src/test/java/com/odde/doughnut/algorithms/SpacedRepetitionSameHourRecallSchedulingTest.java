package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.MemoryTracker;
import org.junit.jupiter.api.Test;

class SpacedRepetitionSameHourRecallSchedulingTest
    extends SpacedRepetitionRecallSchedulingTestBase {
  @Test
  void sameHourCorrectRecallGrowsFirstIntervalStabilityToTwentyFive() {
    MemoryTracker memoryTracker = aGradedTrackerAtStability(24f);

    memoryTracker.recalledSuccessfully(sameHourGradeTime(memoryTracker), null);

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

    memoryTracker.recalledSuccessfully(sameHourGradeTime(memoryTracker), null);

    assertThat(memoryTracker.getStability(), equalTo(72.0f));
  }
}
