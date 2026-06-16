package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CronHourTargetDueSelectorTest {

  @Test
  void target_at_cron_hour_start_is_due() {
    Timestamp cronTime = timestamp(2024, 6, 15, 10, 30);

    assertThat(
        CronHourTargetDueSelector.isTargetDueInCronHour(LocalTime.of(10, 0), cronTime), is(true));
  }

  @Test
  void target_at_end_of_cron_hour_is_not_due() {
    Timestamp cronTime = timestamp(2024, 6, 15, 10, 30);

    assertThat(
        CronHourTargetDueSelector.isTargetDueInCronHour(LocalTime.of(11, 0), cronTime), is(false));
  }

  @Test
  void target_in_adjacent_hour_is_not_due() {
    Timestamp cronTime = timestamp(2024, 6, 15, 11, 15);

    assertThat(
        CronHourTargetDueSelector.isTargetDueInCronHour(LocalTime.of(10, 30), cronTime), is(false));
  }

  @Test
  void target_after_midnight_is_due_when_cron_hour_crosses_midnight_start() {
    Timestamp cronTime = timestamp(2024, 6, 16, 0, 30);

    assertThat(
        CronHourTargetDueSelector.isTargetDueInCronHour(LocalTime.of(0, 45), cronTime), is(true));
  }

  @Test
  void target_after_midnight_is_not_due_in_prior_day_hour() {
    Timestamp cronTime = timestamp(2024, 6, 15, 23, 30);

    assertThat(
        CronHourTargetDueSelector.isTargetDueInCronHour(LocalTime.of(0, 45), cronTime), is(false));
  }

  @Test
  void target_in_late_night_hour_is_due() {
    Timestamp cronTime = timestamp(2024, 6, 15, 23, 30);

    assertThat(
        CronHourTargetDueSelector.isTargetDueInCronHour(LocalTime.of(23, 45), cronTime), is(true));
  }

  private static Timestamp timestamp(int year, int month, int day, int hour, int minute) {
    return Timestamp.valueOf(LocalDateTime.of(year, month, day, hour, minute));
  }
}
