package com.odde.doughnut.services;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class TimestampService {
  public Timestamp addHoursToTimestamp(Timestamp timestamp, int hoursToAdd) {
    ZonedDateTime zonedDateTime = timestamp.toInstant().atZone(ZoneId.of("UTC"));
    return Timestamp.from(zonedDateTime.plusHours(hoursToAdd).toInstant());
  }

  public int getDayId(Timestamp timestamp, ZoneId timeZone) {
    ZonedDateTime userLocalDateTime = getZonedDateTime(timestamp, timeZone);
    return userLocalDateTime.getYear() * 366 + userLocalDateTime.getDayOfYear();
  }

  public Timestamp alignByHalfADay(Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final ZonedDateTime alignedZonedDt = alignDayAndHourByHalfADay(currentUTCTimestamp, timeZone);
    return Timestamp.from(alignedZonedDt.withMinute(0).withSecond(0).toInstant());
  }

  private ZonedDateTime alignDayAndHourByHalfADay(Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final ZonedDateTime zonedDateTime = getZonedDateTime(currentUTCTimestamp, timeZone);
    if (zonedDateTime.getHour() < 12) {
      return zonedDateTime.withHour(12);
    }
    return zonedDateTime.plusDays(1).withHour(0);
  }

  public ZonedDateTime getZonedDateTime(Timestamp timestamp, ZoneId timeZone) {
    ZonedDateTime systemLocalDateTime = timestamp.toLocalDateTime().atZone(ZoneId.systemDefault());
    return systemLocalDateTime.withZoneSameInstant(timeZone);
  }

  public long getDiffInHours(Timestamp currentUTCTimestamp, Timestamp nextRecallAt) {
    long diff = currentUTCTimestamp.getTime() - nextRecallAt.getTime();
    return TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS);
  }

  /**
   * Formats ISO time strings with BC/BCE date support. Handles invalid date components (00 for
   * month/day) with simplified output formats.
   *
   * @param inputTime ISO time string (e.g., "2020-01-01T00:00:00Z", "-0044-03-15T00:00:00Z")
   * @return Formatted date string (e.g., "01 January 2020", "January 1170", "1170", "1171 B.C.")
   */
  public String formatISOTimeToYearSupportingBC(String inputTime) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.systemDefault())
            .localizedBy(Locale.ENGLISH);

    String timeStr = inputTime;
    boolean isBC = inputTime.startsWith("-");

    // Extract original year from the input
    int year = 0;
    try {
      String yearPart = inputTime.replaceFirst("^[+-]", "").split("-")[0];
      year = Integer.parseInt(yearPart);
    } catch (Exception e) {
      // Ignore parsing errors
    }

    // Check for special cases with all-zero or partial-zero date components
    if (inputTime.matches(".*-00-00T.*")) {
      // Format like "1170" or "1171 B.C." for dates with both month and day as 00
      return isBC ? (year + 1) + " B.C." : String.valueOf(year);
    } else if (inputTime.matches(".*-\\d{2}-00T.*")) {
      // Format like "January 1170" for dates with only day as 00
      try {
        // Extract month name
        String monthPart = inputTime.replaceFirst("^[+-]?\\d{4}-", "").split("-")[0];
        int monthNum = Integer.parseInt(monthPart);

        if (monthNum > 0 && monthNum <= 12) {
          String monthName =
              java.time.Month.of(monthNum).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
          return isBC ? monthName + " " + (year + 1) + " B.C." : monthName + " " + year;
        }
      } catch (Exception e) {
        // Fall back to default handling if month parsing fails
      }
    }

    // Remove leading '+' or '-' signs
    if (inputTime.startsWith("+") || inputTime.startsWith("-")) {
      timeStr = inputTime.substring(1);
    }

    // Replace invalid "00" month or day with "01" to ensure valid ISO format
    timeStr = timeStr.replaceAll("(\\d{4})-00-", "$1-01-");
    timeStr = timeStr.replaceAll("(\\d{4}-\\d{2})-00", "$1-01");

    try {
      Instant instant = Instant.parse(isBC ? "-" + timeStr : timeStr);

      if (isBC) {
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());

        // For BC dates, add 1 to the year for historical convention
        int bcYear = year + 1;

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

  public Timestamp getStartOfDay(Timestamp timestamp, ZoneId zoneId) {
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
