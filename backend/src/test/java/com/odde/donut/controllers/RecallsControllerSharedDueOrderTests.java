package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.controllers.dto.MemoryTrackerLite;
import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallsControllerSharedDueOrderTests extends RecallsControllerTestBase {

  @Test
  void sameBandPeersCanAppearInDifferentOrdersAcrossRandomSeeds() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    MemoryTracker first = dueTracker(currentTime);
    MemoryTracker second = dueTracker(TimestampOperations.addHoursToTimestamp(currentTime, 1));

    testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.seed, 0));
    List<Integer> orderWithSeed0 =
        controller.recalling("Asia/Shanghai", 0).getToRepeat().stream()
            .map(MemoryTrackerLite::getMemoryTrackerId)
            .toList();

    testabilitySettings.setRandomization(
        new Randomization(Randomization.RandomStrategy.seed, 4096));
    List<Integer> orderWithSeed4096 =
        controller.recalling("Asia/Shanghai", 0).getToRepeat().stream()
            .map(MemoryTrackerLite::getMemoryTrackerId)
            .toList();

    assertThat(orderWithSeed0, hasSize(2));
    assertThat(orderWithSeed4096, hasSize(2));
    assertTrue(orderWithSeed0.containsAll(List.of(first.getId(), second.getId())));
    assertNotEquals(orderWithSeed0, orderWithSeed4096);
  }

  @Test
  void earlierTwelveHourBandPrecededSameBandPeers() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    MemoryTracker laterBand = dueTracker(currentTime);
    MemoryTracker earlierBand =
        dueTracker(TimestampOperations.addHoursToTimestamp(currentTime, -12));

    List<Integer> order =
        controller.recalling("Asia/Shanghai", 0).getToRepeat().stream()
            .map(MemoryTrackerLite::getMemoryTrackerId)
            .toList();

    assertEquals(List.of(earlierBand.getId(), laterBand.getId()), order);
  }

  @Test
  void spellingPreferenceHoldsWithinTwelveHourBand() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    // Stay inside one 12h UTC band (00:00 is a band boundary; -2h and -1h share the prior band).
    MemoryTracker understanding =
        dueTracker(TimestampOperations.addHoursToTimestamp(currentTime, -2));
    MemoryTracker spelling =
        makeMe
            .aMemoryTrackerFor(ownedNote())
            .spelling()
            .nextRecallAt(TimestampOperations.addHoursToTimestamp(currentTime, -1))
            .please();

    List<MemoryTrackerLite> order = controller.recalling("Asia/Shanghai", 0).getToRepeat();

    assertEquals(spelling.getId(), order.get(0).getMemoryTrackerId());
    assertTrue(order.get(0).isSpelling());
    assertEquals(understanding.getId(), order.get(1).getMemoryTrackerId());
  }

  @Test
  void futureDueTrackersStayExcludedFromSharedDueList() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    MemoryTracker due = dueTracker(currentTime);
    dueTracker(TimestampOperations.addHoursToTimestamp(currentTime, 24));

    List<Integer> order =
        controller.recalling("Asia/Shanghai", 0).getToRepeat().stream()
            .map(MemoryTrackerLite::getMemoryTrackerId)
            .toList();

    assertEquals(List.of(due.getId()), order);
  }
}
