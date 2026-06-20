package com.odde.doughnut.algorithms;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Most recent wall-clock occurrence of a target time-of-day at or before a reference instant. */
public final class RecallSilentWindowDueInstant {

  private RecallSilentWindowDueInstant() {}

  public static LocalDateTime lastDueInstantAtOrBefore(
      LocalTime targetTimeOfDay, LocalDateTime now) {
    LocalDateTime todayOccurrence = now.toLocalDate().atTime(targetTimeOfDay);
    if (!todayOccurrence.isAfter(now)) {
      return todayOccurrence;
    }
    return todayOccurrence.minusDays(1);
  }
}
