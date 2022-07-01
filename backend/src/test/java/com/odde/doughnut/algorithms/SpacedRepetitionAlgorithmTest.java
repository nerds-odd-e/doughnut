package com.odde.doughnut.algorithms;

import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;
import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
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

    final int nextForgettingCurveIndex =
        spacedRepetitionAlgorithm.addTotForgettingCurveIndex1(0, 0);
    assertThat(spacedRepetitionAlgorithm.getRepeatInHours(nextForgettingCurveIndex), equalTo(0));
  }

  @Nested
  class ASettingOf379 {
    SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 6, 9");

    @Test
    void repeatBelowDefaultIndex() {
      int index = DEFAULT_FORGETTING_CURVE_INDEX - 2;
      assertThat(spacedRepetitionAlgorithm.getRepeatInHours(index), equalTo(0));
    }

    @Test
    void repeatForTheFirstTime() {
      int index = DEFAULT_FORGETTING_CURVE_INDEX;
      assertThat(spacedRepetitionAlgorithm.getRepeatInHours(index), equalTo(3 * 24));
    }

    @Test
    void betweenTheFirstAndSecond() {
      int index = DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT / 2;
      assertThat(spacedRepetitionAlgorithm.getRepeatInHours(index), greaterThan(3 * 24));
      assertThat(spacedRepetitionAlgorithm.getRepeatInHours(index), lessThan(6 * 24));
    }

    @Test
    void fallOnTheSecondRepeatLevel() {
      int index =
          spacedRepetitionAlgorithm.addTotForgettingCurveIndex1(DEFAULT_FORGETTING_CURVE_INDEX, 1);
      assertThat(spacedRepetitionAlgorithm.getRepeatInHours(index), equalTo(79));
    }
  }
}
