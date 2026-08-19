package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.entities.RecallLogElapsedHoursBackfill.LogRow;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecallLogElapsedHoursBackfillTest {

  private static final int TRACKER_A = 10;
  private static final int TRACKER_B = 20;

  @Test
  void reconstructsFirstMappedNullElapsedAsZero() {
    assertThat(reconstructed(nullElapsed(1, onDay(1), ProductOutcome.GOOD)), hasEntry(1, 0));
  }

  @Test
  void reconstructsLaterMappedNullElapsedFromPreviousMapped() {
    assertThat(
        reconstructed(
            nullElapsed(1, onDay(1), ProductOutcome.GOOD),
            nullElapsed(2, onDay(2), ProductOutcome.HARD)),
        hasEntry(2, 24));
  }

  @Test
  void reconstructsConfusionNullElapsedFromLastMappedWithoutBecomingAnchor() {
    Map<Integer, Integer> updates =
        reconstructed(
            nullElapsed(1, onDay(1), ProductOutcome.GOOD),
            nullElapsed(2, onDay(2), ProductOutcome.CONFUSION),
            nullElapsed(3, onDay(3), ProductOutcome.EASY));

    assertThat(updates, hasEntry(2, 24));
    assertThat(updates, hasEntry(3, 48));
  }

  @Test
  void reconstructsConfusionNullElapsedAsZeroWhenNoMappedGrade() {
    assertThat(reconstructed(nullElapsed(1, onDay(1), ProductOutcome.CONFUSION)), hasEntry(1, 0));
  }

  @Test
  void omitsAlreadySetElapsedAndReconstructsLaterNullFromThatMappedTime() {
    Map<Integer, Integer> updates =
        reconstructed(
            elapsedAlreadySet(1, onDay(1), ProductOutcome.GOOD),
            nullElapsed(2, onDay(2), ProductOutcome.HARD));

    assertThat(updates, not(hasKey(1)));
    assertThat(updates, hasEntry(2, 24));
  }

  @Test
  void reconstructsNullElapsedAsZeroWhenRecordedAtIsBeforeLastMapped() {
    assertThat(
        reconstructed(
            elapsedAlreadySet(1, onDay(2), ProductOutcome.GOOD),
            nullElapsed(2, onDay(1), ProductOutcome.HARD)),
        hasEntry(2, 0));
  }

  @Test
  void reconstructsNullElapsedAsZeroWhenLastMappedIsOnADifferentMemoryTracker() {
    assertThat(
        reconstructed(
            elapsedAlreadySet(1, TRACKER_A, onDay(1), ProductOutcome.GOOD),
            nullElapsed(2, TRACKER_B, onDay(2), ProductOutcome.GOOD)),
        hasEntry(2, 0));
  }

  private static Timestamp onDay(int dayOfMonth) {
    return Timestamp.valueOf("1989-01-%02d 00:00:00".formatted(dayOfMonth));
  }

  private static LogRow nullElapsed(int id, Timestamp recordedAt, ProductOutcome outcome) {
    return nullElapsed(id, TRACKER_A, recordedAt, outcome);
  }

  private static LogRow nullElapsed(
      int id, int trackerId, Timestamp recordedAt, ProductOutcome outcome) {
    return new LogRow(id, trackerId, recordedAt, outcome, null);
  }

  private static LogRow elapsedAlreadySet(int id, Timestamp recordedAt, ProductOutcome outcome) {
    return elapsedAlreadySet(id, TRACKER_A, recordedAt, outcome);
  }

  private static LogRow elapsedAlreadySet(
      int id, int trackerId, Timestamp recordedAt, ProductOutcome outcome) {
    return new LogRow(id, trackerId, recordedAt, outcome, 12);
  }

  private static Map<Integer, Integer> reconstructed(LogRow... rows) {
    return RecallLogElapsedHoursBackfill.reconstructedNullElapsedById(List.of(rows));
  }
}
