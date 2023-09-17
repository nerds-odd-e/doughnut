package com.odde.doughnut.testability.builders;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimestampBuilder {
  private int day;
  private int hour;
  ZoneId userTimeZone = ZoneId.of("UTC");

  public TimestampBuilder of(int day, int hour) {
    this.day = day;
    this.hour = hour;
    return this;
  }

  public TimestampBuilder fromShanghai() {
    userTimeZone = ZoneId.of("Asia/Shanghai");
    return this;
  }

  public Timestamp please() {
    ZonedDateTime userDateTime = ZonedDateTime.of(1989, 1, 1, hour, 0, 0, 0, userTimeZone);
    ZonedDateTime utc = userDateTime.withZoneSameInstant(ZoneId.systemDefault());

    return Timestamp.valueOf(utc.plusDays(day).toLocalDateTime());
  }
}
