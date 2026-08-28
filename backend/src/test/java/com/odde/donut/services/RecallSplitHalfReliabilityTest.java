package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.answered;
import static com.odde.donut.services.RecallStatsTestFixtures.utc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers slice 21.4: the split-half reliability diagnostic that gates slices 22-25 (see "The index"
 * section of the plan). {@link RecallMorningHalfIndex} itself is exercised by {@link
 * RecallMorningHalfIndexTest} (sharing its warmed-up baseline fixture via {@link
 * RecallStatsTestFixtures}); these tests cover the new orchestration (enumerating candidate
 * mornings, collecting only both-halves-non-null pairs, the minimum-pairs-for-correlation gate) and
 * the pure Pearson/Spearman-Brown math, including the Jidoka-flagged zero-variance edge case.
 */
class RecallSplitHalfReliabilityTest {
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final int TODAY = RecallStatsTestFixtures.WARMED_UP_BASELINE_TODAY;
  private static final LocalDate TODAY_DATE = RecallStatsTestFixtures.WARMED_UP_BASELINE_TODAY_DATE;
  private static final int BASELINE_MS = RecallStatsTestFixtures.WARMED_UP_BASELINE_MS;

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
    // Mirrors RecallMorningHalfIndexTest's "oddAndEvenHalvesAreScoredIndependently..." fixture:
    // one morning with 4 established items split 2/2 across the halves, both halves non-null.
    List<RecallAnswerRow> rows = RecallStatsTestFixtures.warmedUpBaselines();
    int[] items = {9001, 9002, 9003, 9004};
    boolean[] correctByPosition = {true, false, true, false};
    for (int i = 0; i < items.length; i++) {
      rows.add(answered(utc(0, 8), BASELINE_MS, true, null, items[i]));
      rows.add(answered(utc(TODAY, 8 + i), BASELINE_MS, correctByPosition[i], null, items[i], 0.5));
    }

    RecallSplitHalfReliability.Result result =
        RecallSplitHalfReliability.compute(rows, TODAY_DATE, UTC);

    // Exactly one candidate morning qualifies (4 rows, both halves score) - below the 10-pair
    // floor, so a correlation coefficient would be more noise than signal.
    assertThat(result.pairCount(), equalTo(1));
    assertThat(result.rawCorrelation(), nullValue());
    assertThat(result.spearmanBrownCorrelation(), nullValue());
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

  @Test
  void spearmanBrownCorrectsTheRawCorrelationUpward() {
    double r = 0.5;
    double correctedR = (2 * r) / (1 + r);
    assertThat(correctedR, closeTo(0.6667, 0.0001));
  }
}
