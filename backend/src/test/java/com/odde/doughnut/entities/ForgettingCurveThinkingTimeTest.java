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
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ForgettingCurveThinkingTimeTest {
  private ForgettingCurve createForgettingCurve() {
    return new ForgettingCurve(
        new SpacedRepetitionAlgorithm(null), DEFAULT_FORGETTING_CURVE_INDEX + 20);
  }

  private float succeededAfterCurrentInterval(ForgettingCurve curve, Integer thinkingTimeMs) {
    return curve.succeeded(curve.getRepeatInHours(), thinkingTimeMs);
  }

  @Test
  void baseCase_shouldHaveZeroAdjustment() {
    ForgettingCurve curve = createForgettingCurve();
    float indexWithoutThinkingTime = succeededAfterCurrentInterval(curve, null);
    float indexWithBaseThinkingTime = succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    assertThat(indexWithBaseThinkingTime, equalTo(indexWithoutThinkingTime));
  }

  @ParameterizedTest
  @CsvSource({
    "10000, greater",
    "0, greater",
    "40000, less",
    "60000, less",
  })
  void thinkingTimeAdjustsIndexRelativeToBase(int thinkingTimeMs, String direction) {
    ForgettingCurve curve = createForgettingCurve();
    float indexWithBase = succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    float indexWithThinkingTime = succeededAfterCurrentInterval(curve, thinkingTimeMs);
    Matcher<Float> matcher =
        "greater".equals(direction) ? greaterThan(indexWithBase) : lessThan(indexWithBase);
    assertThat(indexWithThinkingTime, matcher);
  }

  @Test
  void thinkingTimeAboveMax_shouldBeClamped() {
    ForgettingCurve curve = createForgettingCurve();
    float indexWithMaxThinkingTime = succeededAfterCurrentInterval(curve, MAX_THINKING_TIME_MS);
    float indexWith100Seconds = succeededAfterCurrentInterval(curve, 100000);
    assertThat(indexWith100Seconds, equalTo(indexWithMaxThinkingTime));
  }

  @Test
  void thinkingTimeAdjustmentCombinedWithElapsedTimeDiscount() {
    ForgettingCurve curve = createForgettingCurve();
    float indexAfterCurrentIntervalWithBase =
        succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    float indexWithNoElapsedTimeAtBase = curve.succeeded(0, BASE_THINKING_TIME_MS);
    float indexWithNoElapsedTimeAt10Seconds = curve.succeeded(0, 10000);

    assertThat(indexAfterCurrentIntervalWithBase, greaterThan(indexWithNoElapsedTimeAtBase));
    assertThat(indexWithNoElapsedTimeAt10Seconds, greaterThan(indexWithNoElapsedTimeAtBase));
  }

  @Test
  void veryFastThinkingTime_shouldNotExceedHalfIncrement() {
    ForgettingCurve curve = createForgettingCurve();
    float indexWithBaseThinkingTime = succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    float indexWithVeryFastThinkingTime = succeededAfterCurrentInterval(curve, 1);
    float adjustment = indexWithVeryFastThinkingTime - indexWithBaseThinkingTime;
    assertThat(adjustment, lessThanOrEqualTo((float) DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT / 2));
  }
}
