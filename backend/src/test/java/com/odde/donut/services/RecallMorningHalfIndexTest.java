package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.answered;
import static com.odde.donut.services.RecallStatsTestFixtures.utc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.services.RecallMorningHalfIndex.Half;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers slice 21.3: scoring a single historical morning's odd- or even-indexed attempts (1-indexed
 * by within-day chronological order) through the full accuracy/pace/lapse/consistency -> {@link
 * RecallCognitiveIndex} pipeline, reusing the same trailing baselines a whole-day score would use
 * (they never depend on which half is selected, since the baseline windows already exclude day D's
 * own rows entirely).
 */
class RecallMorningHalfIndexTest {
  private static final int TODAY = RecallStatsTestFixtures.WARMED_UP_BASELINE_TODAY;
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final LocalDate TODAY_DATE = RecallStatsTestFixtures.WARMED_UP_BASELINE_TODAY_DATE;
  private static final int BASELINE_MS = RecallStatsTestFixtures.WARMED_UP_BASELINE_MS;

  @Test
  void aDayWithNoQualifyingRowsYieldsNoIndexForEitherHalf() {
    List<RecallAnswerRow> rows = RecallStatsTestFixtures.warmedUpBaselines();
    assertThat(RecallMorningHalfIndex.compute(rows, TODAY_DATE, UTC, Half.ODD), nullValue());
    assertThat(RecallMorningHalfIndex.compute(rows, TODAY_DATE, UTC, Half.EVEN), nullValue());
  }

  @Test
  void aHalfWithFewerThanTwoResidualsYieldsNoIndex() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    // One established item, answered once more on TODAY -> the only day-D row, position 1 (odd).
    rows.add(answered(utc(0, 6), BASELINE_MS, true, null, 1, 0.5));
    rows.add(answered(utc(TODAY, 10), BASELINE_MS, true, null, 1, 0.5));
    // Consistency needs >= 2 residuals in the half being scored; this half has only one.
    assertThat(RecallMorningHalfIndex.compute(rows, TODAY_DATE, UTC, Half.ODD), nullValue());
  }

  @Test
  void oddAndEvenHalvesAreScoredIndependentlyAndReflectTheirOwnOutcomes() {
    List<RecallAnswerRow> rows = RecallStatsTestFixtures.warmedUpBaselines();
    int[] items = {9001, 9002, 9003, 9004};
    boolean[] correctByPosition = {true, false, true, false}; // odd = correct, even = incorrect
    for (int i = 0; i < items.length; i++) {
      rows.add(answered(utc(0, 8), BASELINE_MS, true, null, items[i]));
      // Answered exactly at its own established baseline -> zero pace residual, so pace/lapse/
      // consistency come out identical between the two halves; only accuracy (zA) differs, by
      // construction, isolating the sign-flip/wiring this slice is responsible for.
      rows.add(answered(utc(TODAY, 8 + i), BASELINE_MS, correctByPosition[i], null, items[i], 0.5));
    }

    Double oddIndex = RecallMorningHalfIndex.compute(rows, TODAY_DATE, UTC, Half.ODD);
    Double evenIndex = RecallMorningHalfIndex.compute(rows, TODAY_DATE, UTC, Half.EVEN);

    assertThat(oddIndex, notNullValue());
    assertThat(evenIndex, notNullValue());
    // zA_odd = -(1.0 / sqrt(0.5)) = -sqrt(2); zA_even = +sqrt(2); pace/lapse/consistency are
    // identical between halves by construction, so they cancel: difference = -10/4 * (zA_odd -
    // zA_even) = 2.5 * 2*sqrt(2) = 5*sqrt(2).
    assertThat(oddIndex - evenIndex, closeTo(5 * Math.sqrt(2), 0.001));
  }
}
