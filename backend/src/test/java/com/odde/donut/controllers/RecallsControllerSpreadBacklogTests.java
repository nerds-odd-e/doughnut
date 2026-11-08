package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.donut.controllers.dto.MemoryTrackerLite;
import com.odde.donut.controllers.dto.RecallBacklogForecast;
import com.odde.donut.controllers.dto.RecallStatsDTO.DayCount;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecallsControllerSpreadBacklogTests extends RecallsControllerTestBase {
  private static final String SHANGHAI = "Asia/Shanghai";

  @BeforeEach
  void clickAtNineInTheMorning() {
    testabilitySettings.timeTravelTo(shanghai(0, 9));
  }

  private Timestamp shanghai(int day, int hour) {
    return makeMe.aTimestamp().of(day, hour).fromShanghai().please();
  }

  private RecallBacklogForecast spread() {
    return controller.spreadOutBacklog(SHANGHAI);
  }

  private static List<Integer> counts(RecallBacklogForecast forecast) {
    return forecast.days().stream().map(DayCount::getCount).toList();
  }

  private List<Integer> recallListAt(Timestamp time) {
    testabilitySettings.timeTravelTo(time);
    return controller.recalling(SHANGHAI, 0).getToRepeat().stream()
        .map(MemoryTrackerLite::getMemoryTrackerId)
        .toList();
  }

  @Test
  void backlogFillsTodayAndEveryDayToOneLevel() {
    IntStream.range(0, 31).forEach(i -> dueTracker(shanghai(0, 8)));

    RecallBacklogForecast forecast = spread();

    assertEquals(1, forecast.todayCount());
    assertEquals(Collections.nCopies(30, 1), counts(forecast));
    assertEquals(1.0, forecast.averageCount());
    assertEquals("1989-01-02", forecast.days().get(0).getDate());
  }

  @Test
  void earlierDaysTakeTheExtraWhenTheBacklogDoesNotDivideEvenly() {
    IntStream.range(0, 33).forEach(i -> dueTracker(shanghai(0, 8)));

    RecallBacklogForecast forecast = spread();

    assertEquals(2, forecast.todayCount());
    assertEquals(List.of(2, 1, 1), counts(forecast).subList(0, 3));
  }

  @Test
  void heavyFutureDayKeepsItsLoadAndGetsNothing() {
    MemoryTracker heavy = dueTracker(shanghai(1, 10));
    dueTracker(shanghai(1, 11));
    IntStream.range(0, 3).forEach(i -> dueTracker(shanghai(0, 8)));

    RecallBacklogForecast forecast = spread();

    assertEquals(1, forecast.todayCount());
    assertEquals(List.of(2, 1, 1, 0), counts(forecast).subList(0, 4));
    assertEquals(4.0 / 30, forecast.averageCount(), 1e-9);
    assertEquals(shanghai(1, 10), heavy.getNextRecallAt());
  }

  @Test
  void nothingMovesWhenTodayIsWithinTheLevel() {
    MemoryTracker today = dueTracker(shanghai(-1, 20));
    dueTracker(shanghai(2, 0));
    makeMe
        .aMemoryTrackerBy(currentUser.getUser())
        .removedFromTracking()
        .nextRecallAt(shanghai(0, 8))
        .please();

    RecallBacklogForecast forecast = spread();

    assertEquals(1, forecast.todayCount());
    assertEquals(List.of(1, 0), counts(forecast).subList(0, 2));
    assertEquals(shanghai(-1, 20), today.getNextRecallAt());
  }

  @Test
  void movedItemIsDueInTomorrowMorningsRecallNotTonights() {
    MemoryTracker overdue = dueTracker(shanghai(-1, 20));
    MemoryTracker laterToday = dueTracker(shanghai(0, 20));

    spread();

    assertEquals(shanghai(1, 1), laterToday.getNextRecallAt());
    assertThat(recallListAt(shanghai(0, 18)), contains(overdue.getId()));
    assertThat(recallListAt(shanghai(1, 9)), hasItem(laterToday.getId()));
  }

  @Test
  void onlyTheDueTimeChanges() throws UnexpectedNoAccessRightException {
    dueTracker(shanghai(-1, 20));
    MemoryTracker moved =
        makeMe
            .aMemoryTrackerBy(currentUser.getUser())
            .lastRecalledAt(shanghai(-5, 9))
            .difficulty(6.5f)
            .recallCount(2)
            .nextRecallAt(shanghai(0, 20))
            .please();
    Float stability = moved.getStability();

    spread();
    makeMe.refresh(moved);

    assertEquals(shanghai(1, 1), moved.getNextRecallAt());
    assertEquals(stability, moved.getStability());
    assertEquals(6.5f, moved.getDifficulty());
    assertEquals(2, recallLogsOf(moved).size());
  }
}
