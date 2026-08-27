package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the accuracy tile's standardized Poisson-binomial residual {@code A = Σ(y−p̂) /
 * √Σp̂(1−p̂)} on raw FSRS retrievability. Split out to keep {@link RecallStatsServiceTest} under
 * the 250-line limit, matching the pace/lapse/consistency test-file precedent.
 */
class RecallStatsServiceAccuracyAggregationTest {
  @Test
  void singleCorrectAnswerYieldsPositiveStandardizedResidual() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows = List.of(answered(utc(11, 10), 5000, true, null, 1, 0.5));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(1));
    // (1 - 0.5) / sqrt(0.5 * 0.5) = 0.5 / 0.5 = 1.0
    assertThat(accuracy.getStandardizedResidual(), closeTo(1.0, 0.001));
  }

  @Test
  void singleIncorrectAnswerYieldsNegativeStandardizedResidual() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows = List.of(answered(utc(11, 10), 5000, false, null, 2, 0.5));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(1));
    // (0 - 0.5) / sqrt(0.25) = -1.0
    assertThat(accuracy.getStandardizedResidual(), closeTo(-1.0, 0.001));
  }

  @Test
  void mixedOutcomesCombineAcrossTheDenominatorAndNumerator() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(11, 9), 5000, true, null, 3, 0.8),
            answered(utc(11, 10), 5000, false, null, 4, 0.8));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(2));
    // sum residual = (1-0.8) + (0-0.8) = -0.6; sum variance = 0.16 + 0.16 = 0.32
    // A = -0.6 / sqrt(0.32) = -1.06066
    assertThat(accuracy.getStandardizedResidual(), closeTo(-1.06066, 0.001));
  }

  @Test
  void rowsWithNullRetrievabilityAreExcludedFromTheSumAndSampleSize() {
    Timestamp now = utc(11, 12);
    List<RecallAnswerRow> rows =
        List.of(
            // no persisted retrievability (e.g. a New tracker's first grade) -> excluded
            answered(utc(11, 9), 5000, true, null, 5, null),
            answered(utc(11, 10), 5000, true, null, 6, 0.5));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(1));
    assertThat(accuracy.getStandardizedResidual(), closeTo(1.0, 0.001));
  }

  @Test
  void statisticIsNullWhenThereAreNoQualifyingRows() {
    Timestamp now = utc(11, 12);
    RecallStatsDTO dto = aggregate(List.of(), now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(0));
    assertThat(accuracy.getStandardizedResidual(), nullValue());
  }

  @Test
  void statisticIsNullWhenTheDenominatorIsZero() {
    Timestamp now = utc(11, 12);
    // every retrievability is 1.0 (a dead cert) -> p(1-p) = 0 for every row -> zero denominator
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(11, 9), 5000, true, null, 7, 1.0),
            answered(utc(11, 10), 5000, true, null, 8, 1.0));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(2));
    assertThat(accuracy.getStandardizedResidual(), nullValue());
  }

  @Test
  void implausiblyFastMistapIsExcludedFromAccuracyLikeItIsFromPaceAndRetention() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    List<RecallAnswerRow> rows =
        List.of(
            // establish a ~20000ms baseline for item 9 -> item floor = max(300, 0.25*20000) =
            // 5000ms
            answered(utc(9, 10), 20000, true, null, 9, null),
            answered(utc(9, 11), 20000, true, null, 9, null),
            // implausibly fast mistap today: has a persisted retrievability, but must still be
            // excluded from the accuracy sum since a lucky mistap isn't a genuine observation
            answered(utc(11, 9), 1500, true, null, 9, 0.9),
            // a normal-speed answer today
            answered(utc(11, 10), 20000, true, null, 9, 0.5));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(1));
    assertThat(accuracy.getStandardizedResidual(), closeTo(1.0, 0.001));
  }
}
