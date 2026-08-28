package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.services.RecallMorningHalfIndex.Half;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers slice 21.4: the split-half reliability diagnostic that gates slices 22-25 (see "The index"
 * section of the plan). {@link RecallMorningHalfIndex} itself is exercised by {@link
 * RecallMorningHalfIndexTest} (sharing its warmed-up baseline fixture via {@link
 * RecallStatsTestFixtures}); these tests cover the orchestration (enumerating candidate mornings,
 * collecting only both-halves-non-null pairs, the minimum-pairs-for-correlation gate) and the
 * Pearson/Spearman-Brown path that actually produces a coefficient once enough pairs exist.
 */
class RecallSplitHalfReliabilityTest {
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final int TODAY = RecallStatsTestFixtures.WARMED_UP_BASELINE_TODAY;
  private static final LocalDate TODAY_DATE = RecallStatsTestFixtures.WARMED_UP_BASELINE_TODAY_DATE;

  @Test
  void noHistoryYieldsNoPairsAndNullCorrelations() {
    RecallSplitHalfReliability.Result result =
        RecallSplitHalfReliability.compute(List.of(), TODAY_DATE, UTC);
    assertThat(result.pairCount(), equalTo(0));
    assertThat(result.rawCorrelation(), nullValue());
    assertThat(result.spearmanBrownCorrelation(), nullValue());
  }

  @Test
  void aSingleQualifyingMorningIsCountedButFallsBelowTheMinimumPairsForCorrelation() {
    // One morning with 4 established items split 2/2 across the halves, both halves non-null.
    List<RecallAnswerRow> rows = RecallStatsTestFixtures.warmedUpBaselines();
    RecallStatsTestFixtures.addScorableMorning(
        rows, TODAY, 9001, new boolean[] {true, false, true, false});

    RecallSplitHalfReliability.Result result =
        RecallSplitHalfReliability.compute(rows, TODAY_DATE, UTC);

    // Exactly one candidate morning qualifies (4 rows, both halves score) - below the 10-pair
    // floor, so a correlation coefficient would be more noise than signal.
    assertThat(result.pairCount(), equalTo(1));
    assertThat(result.rawCorrelation(), nullValue());
    assertThat(result.spearmanBrownCorrelation(), nullValue());
  }

  @Test
  void tenScorableMorningsYieldThePearsonOfTheirHalfIndexesAndTheSpearmanBrownCorrection() {
    // Varied per-day pace/lapse (not the two-value even/odd pattern) so later mornings that fall
    // in an earlier morning's trailing window cannot collapse baseline MAD to 0.
    List<RecallAnswerRow> rows = RecallStatsTestFixtures.variedBaselinesThrough(TODAY - 20);
    // Vary odd/even outcomes so neither half-index series is constant (Pearson undefined).
    for (int d = 0; d < 10; d++) {
      boolean oddCorrect = d % 2 == 0;
      boolean evenCorrect = d % 3 != 0;
      RecallStatsTestFixtures.addScorableMorning(
          rows,
          TODAY - 9 + d,
          9100 + d * 4,
          new boolean[] {oddCorrect, evenCorrect, oddCorrect, evenCorrect});
    }

    List<double[]> expectedPairs = new ArrayList<>();
    for (int d = 0; d < 10; d++) {
      int dayNumber = TODAY - 9 + d;
      LocalDate day = TODAY_DATE.minusDays(9 - d);
      Double odd = RecallMorningHalfIndex.compute(rows, day, UTC, Half.ODD);
      Double even = RecallMorningHalfIndex.compute(rows, day, UTC, Half.EVEN);
      assertThat("odd half-index for day " + dayNumber, odd, notNullValue());
      assertThat("even half-index for day " + dayNumber, even, notNullValue());
      expectedPairs.add(new double[] {odd, even});
    }
    Double expectedR = RecallSplitHalfReliability.pearson(expectedPairs);
    assertThat(expectedR, notNullValue());

    RecallSplitHalfReliability.Result result =
        RecallSplitHalfReliability.compute(rows, TODAY_DATE, UTC);

    assertThat(result.pairCount(), greaterThanOrEqualTo(10));
    assertThat(result.pairCount(), equalTo(expectedPairs.size()));
    assertThat(result.rawCorrelation(), closeTo(expectedR, 0.0001));
    assertThat(
        result.spearmanBrownCorrelation(), closeTo((2 * expectedR) / (1 + expectedR), 0.0001));
  }

  @Test
  void pearsonOfAPerfectLinearRelationshipIsOne() {
    List<double[]> pairs = List.of(new double[] {1, 2}, new double[] {2, 4}, new double[] {3, 6});
    assertThat(RecallSplitHalfReliability.pearson(pairs), closeTo(1.0, 0.0001));
  }

  @Test
  void pearsonIsNullWhenOneSeriesHasZeroVariance() {
    // Jidoka-flagged edge case: correlation is mathematically undefined, not zero, when a series
    // never varies across the sample.
    List<double[]> pairs = List.of(new double[] {5, 1}, new double[] {5, 2}, new double[] {5, 3});
    assertThat(RecallSplitHalfReliability.pearson(pairs), nullValue());
  }
}
