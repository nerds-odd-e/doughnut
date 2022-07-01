package com.odde.doughnut.entities;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;

public class ForgettingCurve {
  public static final Integer DEFAULT_FORGETTING_CURVE_INDEX = 100;
  public static final Integer DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT = 10;
  private SpacedRepetitionAlgorithm spacedRepetitionAlgorithm;
  private Integer forgettingCurveIndex;

  public ForgettingCurve(
      SpacedRepetitionAlgorithm spacedRepetitionAlgorithm, Integer forgettingCurveIndex) {
    this.spacedRepetitionAlgorithm = spacedRepetitionAlgorithm;
    this.forgettingCurveIndex = forgettingCurveIndex;
  }

  private int add(int adjustment) {
    int newIndex = forgettingCurveIndex + adjustment;
    if (newIndex < DEFAULT_FORGETTING_CURVE_INDEX) {
      newIndex = DEFAULT_FORGETTING_CURVE_INDEX;
    }
    return newIndex;
  }

  public Integer getRepeatInHours() {
    float index =
        (float) (forgettingCurveIndex - DEFAULT_FORGETTING_CURVE_INDEX)
            / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    return spacedRepetitionAlgorithm.getRepeatInHours(index);
  }

  int succeeded(long delayInHours) {
    int delayAdjustment = DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    Integer oldRepeatInHours = getRepeatInHours();
    if (oldRepeatInHours > 0) {
      delayAdjustment =
          (int)
              (DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT
                  - Math.abs(delayInHours)
                      * DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT
                      / oldRepeatInHours);
    }
    return add(delayAdjustment);
  }

  public int failed() {
    return add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2);
  }
}
