package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.ForgettingCurve.BASE_THINKING_TIME_MS;
import static com.odde.doughnut.entities.ForgettingCurve.MAX_THINKING_TIME_MS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ForgettingCurveThinkingTimeTest {
  private static final float ON_TIME_STABILITY_HOURS = 24f;

  private ForgettingCurve createForgettingCurve() {
    return new ForgettingCurve(ON_TIME_STABILITY_HOURS);
  }

  private float succeededAfterCurrentInterval(ForgettingCurve curve, Integer thinkingTimeMs) {
    return curve.succeeded(Math.round(ON_TIME_STABILITY_HOURS), thinkingTimeMs);
  }

  @Test
  void baseCase_shouldHaveZeroAdjustment() {
    ForgettingCurve curve = createForgettingCurve();
    float hoursWithoutThinkingTime = succeededAfterCurrentInterval(curve, null);
    float hoursWithBaseThinkingTime = succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    assertThat(hoursWithBaseThinkingTime, equalTo(hoursWithoutThinkingTime));
  }

  @ParameterizedTest
  @CsvSource({
    "10000, greater",
    "0, greater",
    "40000, less",
    "60000, less",
  })
  void thinkingTimeAdjustsHoursRelativeToBase(int thinkingTimeMs, String direction) {
    ForgettingCurve curve = createForgettingCurve();
    float hoursWithBase = succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    float hoursWithThinkingTime = succeededAfterCurrentInterval(curve, thinkingTimeMs);
    Matcher<Float> matcher =
        "greater".equals(direction) ? greaterThan(hoursWithBase) : lessThan(hoursWithBase);
    assertThat(hoursWithThinkingTime, matcher);
  }

  @Test
  void thinkingTimeAboveMax_shouldBeClamped() {
    ForgettingCurve curve = createForgettingCurve();
    float hoursWithMaxThinkingTime = succeededAfterCurrentInterval(curve, MAX_THINKING_TIME_MS);
    float hoursWith100Seconds = succeededAfterCurrentInterval(curve, 100000);
    assertThat(hoursWith100Seconds, equalTo(hoursWithMaxThinkingTime));
  }

  @Test
  void thinkingTimeAdjustmentCombinedWithSameHourRecall() {
    ForgettingCurve curve = createForgettingCurve();
    float hoursAfterCurrentIntervalWithBase =
        succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    float hoursWithNoElapsedTimeAtBase = curve.succeeded(0, BASE_THINKING_TIME_MS);
    float hoursWithNoElapsedTimeAtFastThinking = curve.succeeded(0, 0);

    assertThat(hoursAfterCurrentIntervalWithBase, greaterThan(hoursWithNoElapsedTimeAtBase));
    assertThat(hoursWithNoElapsedTimeAtFastThinking, greaterThan(hoursWithNoElapsedTimeAtBase));
  }

  @Test
  void veryFastThinkingTime_shouldNotExceedHalfIncrement() {
    ForgettingCurve curve = createForgettingCurve();
    float hoursWithBaseThinkingTime = succeededAfterCurrentInterval(curve, BASE_THINKING_TIME_MS);
    float hoursWithVeryFastThinkingTime = succeededAfterCurrentInterval(curve, 1);
    float adjustment = hoursWithVeryFastThinkingTime - hoursWithBaseThinkingTime;
    float standardIncrement = hoursWithBaseThinkingTime - ON_TIME_STABILITY_HOURS;
    assertThat(adjustment, lessThanOrEqualTo(standardIncrement / 2));
  }
}
