package com.odde.doughnut.algorithms;

import java.sql.Timestamp;
import java.time.LocalTime;

/** Nanosecond-precision time-of-day on a rolling 24-hour recall clock. */
final class RecallTimeOfDay {

  static final long NANOS_PER_HOUR = 60L * 60 * 1_000_000_000L;
  static final long NANOS_PER_DAY = 24L * NANOS_PER_HOUR;

  private RecallTimeOfDay() {}

  static LocalTime fromTimestamp(Timestamp timestamp) {
    return timestamp.toLocalDateTime().toLocalTime();
  }
}
