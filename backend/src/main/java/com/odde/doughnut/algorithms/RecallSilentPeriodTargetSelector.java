package com.odde.doughnut.algorithms;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Longest circular no-recall span on a rolling 24-hour clock; target is one hour after its start.
 */
public final class RecallSilentPeriodTargetSelector {

  private RecallSilentPeriodTargetSelector() {}

  public static LocalTime targetTimeOfDay(Collection<LocalTime> recallTimesOfDay) {
    if (recallTimesOfDay == null || recallTimesOfDay.isEmpty()) {
      throw new IllegalArgumentException("At least one recall time-of-day is required");
    }

    TreeSet<Long> recallNanos = new TreeSet<>();
    for (LocalTime recallTime : recallTimesOfDay) {
      recallNanos.add(recallTime.toNanoOfDay());
    }

    long longestGapNanos = -1;
    long earliestTargetNanos = Long.MAX_VALUE;

    for (long recallNano : recallNanos) {
      Long nextRecallNano = recallNanos.higher(recallNano);
      long gapNanos =
          nextRecallNano == null
              ? RecallTimeOfDay.NANOS_PER_DAY - recallNano + recallNanos.first()
              : nextRecallNano - recallNano;
      long targetNanos =
          (recallNano + RecallTimeOfDay.NANOS_PER_HOUR) % RecallTimeOfDay.NANOS_PER_DAY;
      if (gapNanos > longestGapNanos
          || (gapNanos == longestGapNanos && targetNanos < earliestTargetNanos)) {
        longestGapNanos = gapNanos;
        earliestTargetNanos = targetNanos;
      }
    }

    return LocalTime.ofNanoOfDay(earliestTargetNanos);
  }

  public static LocalTime targetTimeOfDayFromTimestamps(Collection<Timestamp> recallTimestamps) {
    if (recallTimestamps == null || recallTimestamps.isEmpty()) {
      throw new IllegalArgumentException("At least one recall timestamp is required");
    }
    return targetTimeOfDay(recallTimestamps.stream().map(RecallTimeOfDay::fromTimestamp).toList());
  }
}
