package com.odde.doughnut.algorithms;

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
  @Test
  void onTimeCorrectGrowsToNextLadderHours() {
    float hours = nextStabilityHours(72);
    assertThat(hours, equalTo(120.0f));
  }

  @Test
  void overdueCorrectEqualsOnTime() {
    float onTime = nextStabilityHours(72);
    float overdue = nextStabilityHours(72 + 48);
    assertThat(overdue, equalTo(onTime));
  }

  @Test
  void immediateEarlyCorrectDoesNotGrow() {
    float hours = nextStabilityHours(0);
    assertThat(hours, equalTo(72.0f));
  }

  @Test
  void earlyCorrectGrowsLessThanOnTime() {
    float hours = nextStabilityHours(1);
    assertThat(hours, greaterThanOrEqualTo(72.0f));
    assertThat(hours, lessThan(120.0f));
  }

  @Test
  void almostOnTimeEarlyCorrectGrowsLessThanOrEqualToOnTime() {
    float hours = nextStabilityHours(71);
    assertThat(hours, greaterThan(72.0f));
    assertThat(hours, lessThanOrEqualTo(120.0f));
  }

  private float nextStabilityHours(int elapsedInHours) {
    MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
    User user = makeMe.aUser().inMemoryPlease();
    Note note = makeMe.aNote().inMemoryPlease();
    MemoryTracker memoryTracker =
        makeMe.aMemoryTrackerFor(note).by(user).afterNthStrictRecall(3).inMemoryPlease();
    memoryTracker.recalledSuccessfully(
        TimestampOperations.addHoursToTimestamp(memoryTracker.getLastRecalledAt(), elapsedInHours),
        null);
    return memoryTracker.getStability();
  }
}
