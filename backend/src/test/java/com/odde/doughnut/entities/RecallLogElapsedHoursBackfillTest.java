package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

import com.odde.doughnut.entities.RecallLogElapsedHoursBackfill.LogRow;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecallLogElapsedHoursBackfillTest {

  @Test
  void reconstructsFirstMappedNullElapsedAsZero() {
    Timestamp firstAt = onDay(1);

    assertThat(reconstructed(nullElapsed(1, firstAt, ProductOutcome.GOOD)), hasEntry(1, 0));
  }

  @Test
  void reconstructsLaterMappedNullElapsedFromPreviousMapped() {
    Timestamp firstAt = onDay(1);
    Timestamp laterAt = onDay(2);

    assertThat(
        reconstructed(
            nullElapsed(1, firstAt, ProductOutcome.GOOD),
            nullElapsed(2, laterAt, ProductOutcome.HARD)),
        hasEntry(2, 24));
  }

  @Test
  void reconstructsConfusionNullElapsedFromLastMappedWithoutBecomingAnchor() {
    Timestamp mappedAt = onDay(1);
    Timestamp confusionAt = onDay(2);
    Timestamp laterMappedAt = onDay(3);

    Map<Integer, Integer> updates =
        reconstructed(
            nullElapsed(1, mappedAt, ProductOutcome.GOOD),
            nullElapsed(2, confusionAt, ProductOutcome.CONFUSION),
            nullElapsed(3, laterMappedAt, ProductOutcome.EASY));

    assertThat(updates, hasEntry(2, 24));
    assertThat(updates, hasEntry(3, 48));
  }

  @Test
  void reconstructsConfusionNullElapsedAsZeroWhenNoMappedGrade() {
    Timestamp confusionAt = onDay(1);

    assertThat(
        reconstructed(nullElapsed(1, confusionAt, ProductOutcome.CONFUSION)), hasEntry(1, 0));
  }

  private static Timestamp onDay(int dayOfMonth) {
    return Timestamp.valueOf("1989-01-%02d 00:00:00".formatted(dayOfMonth));
  }

  private static LogRow nullElapsed(int id, Timestamp recordedAt, ProductOutcome outcome) {
    return new LogRow(id, 10, recordedAt, outcome, null);
  }

  private static Map<Integer, Integer> reconstructed(LogRow... rows) {
    return RecallLogElapsedHoursBackfill.reconstructedNullElapsedById(List.of(rows));
  }
}
