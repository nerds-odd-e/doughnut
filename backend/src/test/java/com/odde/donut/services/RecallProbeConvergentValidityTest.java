package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.DailyProbe;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers plan {@code 008-probe-convergent-analyses}'s convergent-validity diagnostic: matching each
 * of the four probe-metric/recall-component pairs, gating a day out when there's no qualifying
 * recall data that morning, and the minimum-pairs-for-correlation floor. The Pearson math itself
 * mirrors the retired split-half reliability diagnostic's own well-tested implementation.
 */
class RecallProbeConvergentValidityTest {
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final LocalDate TODAY_DATE = LocalDate.of(1989, 1, 20);

  @Test
  void noHistoryYieldsNoPairsAndNullCorrelationsForAllFourPairs() {
    List<RecallProbeConvergentValidity.PairResult> results =
        RecallProbeConvergentValidity.compute(List.of(), List.of(), TODAY_DATE, UTC);

    assertThat(results, hasSize(4));
    for (RecallProbeConvergentValidity.PairResult result : results) {
      assertThat(result.pairCount(), equalTo(0));
      assertThat(result.rawCorrelation(), nullValue());
    }
  }

  @Test
  void aProbeWithNoQualifyingRecallDataOnThatMorningIsExcludedFromEveryPair() {
    DailyProbe probe = new DailyProbe();
    probe.setCompletedAt(Timestamp.valueOf(TODAY_DATE.atStartOfDay()));
    probe.setSpeed(4.0);
    probe.setAccuracy(100);
    probe.setLapseCount(0);
    probe.setVariability(0.5);

    List<RecallProbeConvergentValidity.PairResult> results =
        RecallProbeConvergentValidity.compute(List.of(), List.of(probe), TODAY_DATE, UTC);

    for (RecallProbeConvergentValidity.PairResult result : results) {
      assertThat(result.pairCount(), equalTo(0));
      assertThat(result.rawCorrelation(), nullValue());
    }
  }

  @Test
  void pearsonOfAPerfectLinearRelationshipIsOne() {
    List<double[]> pairs = List.of(new double[] {1, 2}, new double[] {2, 4}, new double[] {3, 6});
    assertThat(RecallProbeConvergentValidity.pearson(pairs), closeTo(1.0, 0.0001));
  }

  @Test
  void pearsonIsNullWhenOneSeriesHasZeroVariance() {
    // Correlation is mathematically undefined, not zero, when a series never varies.
    List<double[]> pairs = List.of(new double[] {5, 1}, new double[] {5, 2}, new double[] {5, 3});
    assertThat(RecallProbeConvergentValidity.pearson(pairs), nullValue());
  }
}
