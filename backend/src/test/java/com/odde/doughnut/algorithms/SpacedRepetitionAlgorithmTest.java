package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

public class SpacedRepetitionAlgorithmTest {

  @Test
  void legacyConversionUsesUserDayListThenDefault() {
    assertThat(SpacedRepetitionAlgorithm.hoursFromLegacyIndex(100, List.of()), equalTo(0));
    assertThat(
        SpacedRepetitionAlgorithm.hoursFromLegacyIndex(100, List.of(3, 6, 9)), equalTo(3 * 24));
    assertThat(
        SpacedRepetitionAlgorithm.hoursFromLegacyIndex(110, List.of(3, 6, 9)), equalTo(6 * 24));
  }
}
