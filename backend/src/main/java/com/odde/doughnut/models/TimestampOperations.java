package com.odde.doughnut.models;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class TimestampOperations {
  public static Timestamp addHoursToTimestamp(Timestamp timestamp, int hoursToAdd) {
    ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(ZoneId.of("UTC"));
    return Timestamp.from(zonedDateTime.plusHours(hoursToAdd).toInstant());
  }

  public static int getDayId(Timestamp timestamp, ZoneId timeZone) {
    ZonedDateTime userLocalDateTime = TimestampOperations.getZonedDateTime(timestamp, timeZone);
    return userLocalDateTime.getYear() * 366 + userLocalDateTime.getDayOfYear();
  }

  public static Timestamp alignByHalfADay(Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final ZonedDateTime alignedZonedDt = alignDayAndHourByHalfADay(currentUTCTimestamp, timeZone);
    return Timestamp.from(alignedZonedDt.withMinute(0).withSecond(0).toInstant());
  }

  private static ZonedDateTime alignDayAndHourByHalfADay(
      Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final ZonedDateTime zonedDateTime =
        TimestampOperations.getZonedDateTime(currentUTCTimestamp, timeZone);
    if (zonedDateTime.getHour() < 12) {
      return zonedDateTime.withHour(12);
    }
    return zonedDateTime.plusDays(1).withHour(0);
  }

  public static ZonedDateTime getZonedDateTime(Timestamp timestamp, ZoneId timeZone) {
    ZonedDateTime systemLocalDateTime = timestamp.toLocalDateTime().atZone(ZoneId.systemDefault());
    return systemLocalDateTime.withZoneSameInstant(timeZone);
  }

  public static long getDiffInHours(Timestamp currentUTCTimestamp, Timestamp nextRecallAt) {
    long diff = currentUTCTimestamp.getTime() - nextRecallAt.getTime();
    return TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS);
  }

  /**
   * Formats ISO time strings with BC/BCE date support. Handles invalid date components (00 for
   * month/day) and non-standard date formats.
   *
   * @param inputTime ISO time string (e.g., "2020-01-01T00:00:00Z", "-0044-03-15T00:00:00Z",
   *     "+1980-03-31T00:00:00Z")
   * @return Formatted date string (e.g., "01 January 2020", "15 March 0045 B.C.")
   */
  public static String formatISOTimeToYearSupportingBC(String inputTime) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.systemDefault())
            .localizedBy(Locale.ENGLISH);

    String timeStr = inputTime;
    boolean isBC = inputTime.startsWith("-");

    // Remove leading '+' or '-' signs
    if (inputTime.startsWith("+") || inputTime.startsWith("-")) {
      timeStr = inputTime.substring(1);
    }

    // Extract year from the original input
    int originalYear = 0;
    if (isBC) {
      try {
        // Extract year from pattern YYYY-MM-DD
        String yearPart = timeStr.split("-")[0];
        originalYear = Integer.parseInt(yearPart);
      } catch (Exception e) {
        // Ignore parsing errors and continue with default handling
      }
    }

    // Replace invalid "00" month or day with "01" to ensure valid ISO format
    timeStr = timeStr.replaceAll("(\\d{4})-00-", "$1-01-");
    timeStr = timeStr.replaceAll("(\\d{4}-\\d{2})-00", "$1-01");

    try {
      Instant instant = Instant.parse(isBC ? "-" + timeStr : timeStr);

      if (isBC) {
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());

        // For BC dates, add 1 to the year to convert from astronomical year to historical year
        // E.g., astronomical year -552 = historical year 553 BC
        int bcYear = originalYear + 1;

        return String.format(
            "%02d %s %04d B.C.",
            dateTime.getDayOfMonth(),
            dateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            bcYear);
      }

      return formatter.format(instant);
    } catch (Exception e) {
      // Fall back to a simple format for parsing errors
      return "Unknown date";
    }
  }

  public static Timestamp getStartOfDay(Timestamp timestamp, ZoneId zoneId) {
    LocalDateTime localDateTime =
        timestamp
            .toInstant()
            .atZone(zoneId)
            .toLocalDateTime()
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

    return Timestamp.from(localDateTime.atZone(zoneId).toInstant());
  }
}
