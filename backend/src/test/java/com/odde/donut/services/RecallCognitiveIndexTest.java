package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import org.junit.jupiter.api.Test;

/**
 * Pure arithmetic coverage of the composite index formula (slice 21.2): {@code 100 − 10 × mean(zA,
 * zPace, zLapse, zConsistency)}. Inputs here are plain doubles standing in for already-signed
 * z-scores — no wiring to real per-morning rows yet (that is slice 21.3).
 */
class RecallCognitiveIndexTest {
  @Test
  void allZeroZScoresYieldTheBaselineIndexOfOneHundred() {
    assertThat(RecallCognitiveIndex.compute(0, 0, 0, 0), closeTo(100.0, 0.0001));
  }

  @Test
  void uniformlyWorseThanUsualZScoresLowerTheIndexByTenTimesTheMean() {
    // mean(1, 1, 1, 1) = 1, so index = 100 - 10*1 = 90.
    assertThat(RecallCognitiveIndex.compute(1, 1, 1, 1), closeTo(90.0, 0.0001));
  }

  @Test
  void uniformlyBetterThanUsualZScoresRaiseTheIndexAboveOneHundred() {
    // mean(-2, -2, -2, -2) = -2, so index = 100 - 10*(-2) = 120.
    assertThat(RecallCognitiveIndex.compute(-2, -2, -2, -2), closeTo(120.0, 0.0001));
  }

  @Test
  void mixedZScoresAverageBeforeRescaling() {
    // mean(2, 0, -1, 3) = 1, so index = 100 - 10*1 = 90.
    assertThat(RecallCognitiveIndex.compute(2, 0, -1, 3), closeTo(90.0, 0.0001));
  }
}
