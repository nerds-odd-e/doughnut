package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SpacedRepetitionAlgorithmTest {

  @Test
  void defaultSetting() {
    SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm(null);

    assertThat(spacedRepetitionAlgorithm.getRepeatInHoursF(0), equalTo(0));
  }

  @Nested
  class ASettingOf369 {
    SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 6, 9");

    @Test
    void repeatBelowDefaultIndex() {
      assertThat(spacedRepetitionAlgorithm.getRepeatInHoursF(-0.5f), equalTo(0));
    }

    @Test
    void repeatForTheFirstTime() {
      assertThat(spacedRepetitionAlgorithm.getRepeatInHoursF(0), equalTo(3 * 24));
    }

    @Test
    void betweenTheFirstAndSecond() {
      assertThat(spacedRepetitionAlgorithm.getRepeatInHoursF(1.5f), greaterThan(6 * 24));
      assertThat(spacedRepetitionAlgorithm.getRepeatInHoursF(1.5f), lessThan(9 * 24));
    }
  }
}
