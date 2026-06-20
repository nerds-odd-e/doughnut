package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RecallSilentWindowDueInstantTest {

  @Test
  void returns_today_when_target_time_has_already_passed() {
    LocalDateTime now = LocalDateTime.of(2024, 6, 15, 11, 0);

    assertThat(
        RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(LocalTime.of(9, 30), now),
        is(LocalDateTime.of(2024, 6, 15, 9, 30)));
  }

  @Test
  void returns_yesterday_when_target_time_has_not_yet_occurred_today() {
    LocalDateTime now = LocalDateTime.of(2024, 6, 15, 8, 0);

    assertThat(
        RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(LocalTime.of(9, 30), now),
        is(LocalDateTime.of(2024, 6, 14, 9, 30)));
  }

  @Test
  void returns_now_when_target_matches_exactly() {
    LocalDateTime now = LocalDateTime.of(2024, 6, 15, 10, 0);

    assertThat(
        RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(LocalTime.of(10, 0), now), is(now));
  }

  @Test
  void wraps_midnight_for_late_night_target_before_occurrence() {
    LocalDateTime now = LocalDateTime.of(2024, 6, 16, 0, 30);

    assertThat(
        RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(LocalTime.of(0, 45), now),
        is(LocalDateTime.of(2024, 6, 15, 0, 45)));
  }

  @Test
  void wraps_midnight_for_late_night_target_after_occurrence() {
    LocalDateTime now = LocalDateTime.of(2024, 6, 16, 1, 0);

    assertThat(
        RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(LocalTime.of(0, 45), now),
        is(LocalDateTime.of(2024, 6, 16, 0, 45)));
  }
}
