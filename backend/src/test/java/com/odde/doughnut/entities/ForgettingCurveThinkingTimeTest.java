package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.ForgettingCurve.BASE_THINKING_TIME_MS;
import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;
import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
import static com.odde.doughnut.entities.ForgettingCurve.MAX_THINKING_TIME_MS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import org.junit.jupiter.api.Test;

public class ForgettingCurveThinkingTimeTest {
  private ForgettingCurve createForgettingCurve(int forgettingCurveIndex) {
    return new ForgettingCurve(new SpacedRepetitionAlgorithm(null), forgettingCurveIndex);
  }

  @Test
  void baseCase_shouldHaveZeroAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithoutThinkingTime = curve.succeeded(0, null);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    assertThat(indexWithBaseThinkingTime, equalTo(indexWithoutThinkingTime));
  }

  @Test
  void fastAnswer_shouldHavePositiveAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    int indexWith10Seconds = curve.succeeded(0, 10000);
    assertThat(indexWith10Seconds, greaterThan(indexWithBaseThinkingTime));
  }

  @Test
  void slowAnswer_shouldHaveNegativeAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    int indexWith40Seconds = curve.succeeded(0, 40000);
    assertThat(indexWith40Seconds, lessThan(indexWithBaseThinkingTime));
  }

  @Test
  void verySlowAnswer_shouldHaveNegativeAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    int indexWithMaxThinkingTime = curve.succeeded(0, MAX_THINKING_TIME_MS);
    assertThat(indexWithMaxThinkingTime, lessThan(indexWithBaseThinkingTime));
  }

  @Test
  void veryFastAnswer_shouldHavePositiveAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    int indexWith0Seconds = curve.succeeded(0, 0);
    assertThat(indexWith0Seconds, greaterThan(indexWithBaseThinkingTime));
  }

  @Test
  void thinkingTimeAboveMax_shouldBeClamped() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithMaxThinkingTime = curve.succeeded(0, MAX_THINKING_TIME_MS);
    int indexWith100Seconds = curve.succeeded(0, 100000);
    assertThat(indexWith100Seconds, equalTo(indexWithMaxThinkingTime));
  }

  @Test
  void nullThinkingTime_shouldHaveNoAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithNull = curve.succeeded(0, null);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    assertThat(indexWithNull, equalTo(indexWithBaseThinkingTime));
  }

  @Test
  void thinkingTimeAdjustmentCombinedWithDelayAdjustment() {
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexOnTimeWithBase = curve.succeeded(0, BASE_THINKING_TIME_MS);
    int indexOnTimeWith10Seconds = curve.succeeded(0, 10000);
    int indexLateWithBase = curve.succeeded(24, BASE_THINKING_TIME_MS);
    int indexLateWith10Seconds = curve.succeeded(24, 10000);

    // Fast thinking time should increase index more than base thinking time
    assertThat(indexOnTimeWith10Seconds, greaterThan(indexOnTimeWithBase));
    assertThat(indexLateWith10Seconds, greaterThan(indexLateWithBase));

    // Late review should have lower index than on-time review
    assertThat(indexOnTimeWithBase, greaterThan(indexLateWithBase));
    assertThat(indexOnTimeWith10Seconds, greaterThan(indexLateWith10Seconds));
  }

  @Test
  void veryFastThinkingTime_shouldNotExceedHalfIncrement() {
    // This test ensures that when BASE_THINKING_TIME_MS is changed in the future,
    // developers will be reminded to adjust the scale if needed
    ForgettingCurve curve = createForgettingCurve(DEFAULT_FORGETTING_CURVE_INDEX + 20);
    int indexWithBaseThinkingTime = curve.succeeded(0, BASE_THINKING_TIME_MS);
    // 0.001 seconds = 1 millisecond
    int indexWithVeryFastThinkingTime = curve.succeeded(0, 1);
    int adjustment = indexWithVeryFastThinkingTime - indexWithBaseThinkingTime;
    // When BASE_THINKING_TIME_MS is changed, the scale may need adjustment
    assertThat(adjustment, lessThanOrEqualTo(DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT / 2));
  }
}
