package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.List;
import org.junit.jupiter.api.Test;

public class SpacedRepetitionAlgorithmTest {

  @Test
  void assimilateIndexIsDueNow() {
    assertThat(SpacedRepetitionAlgorithm.hoursFromSpacingIndex(0), equalTo(0));
  }

  @Test
  void firstRungIsOneDay() {
    assertThat(SpacedRepetitionAlgorithm.hoursFromSpacingIndex(1), equalTo(24));
  }

  @Test
  void plateauAtOneDayThenTwoDays() {
    assertThat(SpacedRepetitionAlgorithm.hoursFromSpacingIndex(2), equalTo(24));
    assertThat(SpacedRepetitionAlgorithm.hoursFromSpacingIndex(3), equalTo(48));
  }

  @Test
  void interpolatesBetweenRungs() {
    int hours = SpacedRepetitionAlgorithm.hoursFromSpacingIndex(3.5f);
    assertThat(hours, greaterThan(48));
    assertThat(hours, lessThan(72));
  }

  @Test
  void legacyConversionUsesUserDayListThenDefault() {
    assertThat(SpacedRepetitionAlgorithm.hoursFromLegacyIndex(100, List.of()), equalTo(0));
    assertThat(
        SpacedRepetitionAlgorithm.hoursFromLegacyIndex(100, List.of(3, 6, 9)), equalTo(3 * 24));
    assertThat(
        SpacedRepetitionAlgorithm.hoursFromLegacyIndex(110, List.of(3, 6, 9)), equalTo(6 * 24));
  }

  @Test
  void inversePrefersHighEndOfPlateauWhenGrowing() {
    assertThat(SpacedRepetitionAlgorithm.spacingIndexFromHours(24, true), equalTo(2.0f));
  }

  @Test
  void inversePrefersLowEndOfPlateauWhenReducing() {
    assertThat(SpacedRepetitionAlgorithm.spacingIndexFromHours(24, false), equalTo(1.0f));
  }
}
