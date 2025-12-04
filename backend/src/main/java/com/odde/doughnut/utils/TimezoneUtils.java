package com.odde.doughnut.utils;

import java.time.ZoneId;

public class TimezoneUtils {
  private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

  public static ZoneId parseTimezone(String timezone) {
    try {
      return ZoneId.of(timezone);
    } catch (Exception e) {
      return DEFAULT_TIMEZONE;
    }
  }
}
