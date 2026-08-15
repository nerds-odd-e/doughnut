package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_DIFFICULTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import org.junit.jupiter.api.Test;

public class SpacedRepetitionEarlyRecallAdjustmentTest {
  private static final float STABILITY_HOURS = 72f;
  private final MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  private final User user = makeMe.aUser().inMemoryPlease();
  private final Note note = makeMe.aNote().inMemoryPlease();

  @Test
  void immediateEarlyCorrectDoesNotGrow() {
    float hours = nextStabilityHours(0);
    assertThat(hours, equalTo(STABILITY_HOURS));
  }

  @Test
  void earlyCorrectGrowsLessThanOnTime() {
    float early = nextStabilityHours(1);
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
    MemoryTracker memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .stabilityAndNextRecallAt(STABILITY_HOURS)
            .difficulty(DEFAULT_DIFFICULTY)
            .inMemoryPlease();
    memoryTracker.recalledSuccessfully(
        TimestampOperations.addHoursToTimestamp(memoryTracker.getLastRecalledAt(), elapsedInHours),
        null);
    return memoryTracker.getStability();
  }
}
