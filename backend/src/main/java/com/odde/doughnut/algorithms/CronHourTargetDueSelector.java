package com.odde.doughnut.algorithms;

import java.sql.Timestamp;
import java.time.LocalTime;

/** Whether a target time-of-day falls in the cron hour containing a timestamp. */
public final class CronHourTargetDueSelector {

  private CronHourTargetDueSelector() {}

  public static boolean isTargetDueInCronHour(LocalTime targetTimeOfDay, Timestamp cronTime) {
    LocalTime cronTimeOfDay = RecallTimeOfDay.fromTimestamp(cronTime);
    long hourStartNanos = cronTimeOfDay.withMinute(0).withSecond(0).withNano(0).toNanoOfDay();
    long hourEndNanos =
        (hourStartNanos + RecallTimeOfDay.NANOS_PER_HOUR) % RecallTimeOfDay.NANOS_PER_DAY;
    long targetNanos = targetTimeOfDay.toNanoOfDay();

    if (hourStartNanos < hourEndNanos) {
      return targetNanos >= hourStartNanos && targetNanos < hourEndNanos;
    }
    return targetNanos >= hourStartNanos || targetNanos < hourEndNanos;
  }
}
