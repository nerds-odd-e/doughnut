package com.odde.doughnut.algorithms;

import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;
import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.junit.jupiter.api.Test;

public class SpacedRepetitionEarlyRewardsAndLatePenaltyTest {
  SpacedRepetitionAlgorithm spacedRepetitionAlgorithm =
      new SpacedRepetitionAlgorithm("3, 6, 9, 12, 15");
  final int currentForgettingCurveIndex =
      DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2;
  final int baselineForgettingCurveIndex =
      DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 3;

  @Test
  void repeatOnTime() {
    int index = getNextForgettingCurveIndexWithDelay(0);
    assertThat(index, equalTo(baselineForgettingCurveIndex));
  }

  @Test
  void repeatEarly_immediatelyWhichIsImpossible() {
    int index = getNextForgettingCurveIndexWithDelay(-6 * 24);
    assertThat(index, equalTo(currentForgettingCurveIndex));
  }

  @Test
  void repeatEarly_inOneHour() {
    int index = getNextForgettingCurveIndexWithDelay(-6 * 24 + 1);
    assertThat(index, greaterThanOrEqualTo(currentForgettingCurveIndex));
    assertThat(index, lessThan(baselineForgettingCurveIndex));
  }

  @Test
  void repeatEarly_OneHourEarlier() {
    int index = getNextForgettingCurveIndexWithDelay(-1);
    assertThat(index, greaterThan(currentForgettingCurveIndex));
    assertThat(index, lessThanOrEqualTo(baselineForgettingCurveIndex));
  }

  @Test
  void repeatLate_byOneHour() {
    int index = getNextForgettingCurveIndexWithDelay(1);
    assertThat(index, greaterThan(currentForgettingCurveIndex));
    assertThat(index, lessThanOrEqualTo(baselineForgettingCurveIndex));
  }

  @Test
  void repeatLate_byOneDay() {
    int index = getNextForgettingCurveIndexWithDelay(24);
    assertThat(index, greaterThan(currentForgettingCurveIndex));
    assertThat(index, lessThan(baselineForgettingCurveIndex));
  }

  @Test
  void repeatLate_by10Days() {
    int index = getNextForgettingCurveIndexWithDelay(10 * 24);
    assertThat(index, lessThan(currentForgettingCurveIndex));
    assertThat(index, greaterThan(DEFAULT_FORGETTING_CURVE_INDEX));
  }

  @Test
  void repeatLate_byOneHundredDays() {
    int index = getNextForgettingCurveIndexWithDelay(100 * 24);
    assertThat(index, equalTo(DEFAULT_FORGETTING_CURVE_INDEX));
  }

  private int getNextForgettingCurveIndexWithDelay(int delayInHours) {
    return spacedRepetitionAlgorithm.addTotForgettingCurveIndex(
        currentForgettingCurveIndex, 1, delayInHours);
  }
}
