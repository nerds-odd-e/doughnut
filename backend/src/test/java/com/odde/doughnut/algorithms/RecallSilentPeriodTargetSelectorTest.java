package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallSilentPeriodTargetSelectorTest {

  @Test
  void one_recall_targets_one_hour_after_recall_time_of_day() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(List.of(LocalTime.of(15, 30))),
        equalTo(LocalTime.of(16, 30)));
  }

  @Test
  void multiple_recalls_targets_after_longest_silent_span() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(
            List.of(LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))),
        equalTo(LocalTime.of(21, 0)));
  }

  @Test
  void span_crossing_midnight_uses_wraparound_gap() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(
            List.of(LocalTime.of(22, 0), LocalTime.of(2, 0))),
        equalTo(LocalTime.of(3, 0)));
  }

  @Test
  void evenly_spaced_recalls_pick_earliest_target_on_tie() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(
            List.of(
                LocalTime.MIDNIGHT, LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0))),
        equalTo(LocalTime.of(1, 0)));
  }

  @Test
  void boundary_time_wraps_target_past_midnight() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(List.of(LocalTime.of(23, 45))),
        equalTo(LocalTime.of(0, 45)));
  }

  @Test
  void tied_longest_spans_pick_earliest_target_time_of_day() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(
            List.of(LocalTime.MIDNIGHT, LocalTime.of(12, 0))),
        equalTo(LocalTime.of(1, 0)));
  }

  @Test
  void duplicate_recall_times_treated_as_single_point() {
    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDay(
            List.of(LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(15, 0))),
        equalTo(LocalTime.of(16, 0)));
  }

  @Test
  void timestamps_extract_time_of_day_without_timezone_conversion() {
    Timestamp recall = Timestamp.valueOf(java.time.LocalDateTime.of(2024, 3, 15, 8, 15, 30));

    assertThat(
        RecallSilentPeriodTargetSelector.targetTimeOfDayFromTimestamps(List.of(recall)),
        equalTo(LocalTime.of(9, 15, 30)));
  }

  @Test
  void empty_recall_list_is_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RecallSilentPeriodTargetSelector.targetTimeOfDay(List.of()));
  }
}
