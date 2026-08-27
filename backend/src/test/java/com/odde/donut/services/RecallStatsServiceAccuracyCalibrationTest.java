package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * End-to-end (through {@link RecallStatsService#aggregateRows}) tests for slice 19's personal
 * recalibration: today's accuracy is scored against {@code p̂_recalibrated}, fitted live on every
 * request from the trailing 180 days, not against raw FSRS retrievability. Split out of {@link
 * RecallStatsServiceAccuracyAggregationTest} to keep each test class under the 250-line limit,
 * matching the pace/exclusion test-file precedent.
 */
class RecallStatsServiceAccuracyCalibrationTest {
  @Test
  void todaysAccuracyIsScoredAgainstTheRecalibratedProbabilityWhenTrailingHistoryIsAbundant() {
    Timestamp now = utc(200, 12); // today = day 200
    List<RecallAnswerRow> rows = new ArrayList<>();
    // Trailing history (day 190, well within the 180-day window and not today): the scheduler is
    // systematically overconfident at retrievability 0.9 (only 50% actually correct there)...
    for (int i = 0; i < 40; i++) {
      rows.add(answered(utc(190, i % 24), 5000, i < 20, null, i, 0.9));
    }
    // ...and systematically underconfident at retrievability 0.5 (80% actually correct there).
    for (int i = 40; i < 80; i++) {
      rows.add(answered(utc(191, i % 24), 5000, i < 72, null, i, 0.5));
    }
    // Today: one correct answer at raw retrievability 0.9 — the same item-relative situation
    // as the overconfident trailing group.
    rows.add(answered(utc(200, 10), 5000, true, null, 999, 0.9));

    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(1));
    // Recalibrated p̂ for raw retrievability 0.9 converges to the trailing group's empirical
    // proportion (~0.5, per RecallCalibrationFitterTest), not the raw 0.9: A = (1-0.5)/sqrt(0.25)
    // = 1.0, not the raw-retrievability value of (1-0.9)/sqrt(0.09) = 0.333.
    assertThat(accuracy.getStandardizedResidual(), closeTo(1.0, 0.05));
  }

  @Test
  void todaysAccuracyUsesRawRetrievabilityWhenTrailingHistoryIsSparse() {
    Timestamp now = utc(11, 12); // today = 1989-01-11
    // Only a handful of trailing rows — far below the minimum calibration sample size, so the
    // fit falls back to the identity mapping and today's accuracy is scored on raw
    // retrievability, exactly as before this slice.
    List<RecallAnswerRow> rows =
        List.of(
            answered(utc(9, 9), 5000, false, null, 1, 0.9),
            answered(utc(9, 10), 5000, true, null, 2, 0.9),
            answered(utc(11, 10), 5000, true, null, 3, 0.9));
    RecallStatsDTO dto = aggregate(rows, now);
    RecallStatsDTO.AccuracyStats accuracy = dto.getAccuracy();
    assertThat(accuracy.getSampleSize(), equalTo(1));
    // (1 - 0.9) / sqrt(0.9 * 0.1) = 0.1 / 0.3 = 0.333
    assertThat(accuracy.getStandardizedResidual(), closeTo(0.333, 0.001));
  }
}
