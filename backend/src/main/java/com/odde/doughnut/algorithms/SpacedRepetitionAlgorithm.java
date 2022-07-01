package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

public class SpacedRepetitionAlgorithm {
  public static final Integer DEFAULT_FORGETTING_CURVE_INDEX = 100;
  public static final Integer DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT = 10;
  public static final List<Integer> DEFAULT_SPACES =
      Arrays.asList(
          0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765,
          10946, 17711, 28657, 46368, 75025);
  private final List<Integer> spaces;

  public SpacedRepetitionAlgorithm(String spaceIntervals) {
    if (!Strings.isEmpty(spaceIntervals)) {
      spaces =
          Arrays.stream(spaceIntervals.split(",\\s*"))
              .map(Integer::valueOf)
              .collect(Collectors.toList());
    } else {
      spaces = new ArrayList<>();
    }
  }

  public static class MemoryStateChange {
    @Getter public final int nextForgettingCurveIndex;

    public MemoryStateChange(int nextForgettingCurveIndex) {
      this.nextForgettingCurveIndex = nextForgettingCurveIndex;
    }
  }

  public int addTotForgettingCurveIndex(
      Integer oldForgettingCurveIndex, int selfEvaluationIndex, long delayInHours) {
    int newIndex =
        oldForgettingCurveIndex
            + withDelayAdjustment(oldForgettingCurveIndex, delayInHours) * selfEvaluationIndex;
    if (newIndex < DEFAULT_FORGETTING_CURVE_INDEX) {
      return DEFAULT_FORGETTING_CURVE_INDEX;
    }
    return newIndex;
  }

  private int withDelayAdjustment(Integer oldForgettingCurveIndex, long delayInHours) {
    int delayAdjustment = DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    Integer oldRepeatInHours = getRepeatInHours(oldForgettingCurveIndex);
    if (oldRepeatInHours > 0) {
      delayAdjustment =
          (int)
              (DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT
                  - Math.abs(delayInHours)
                      * DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT
                      / oldRepeatInHours);
    }
    return delayAdjustment;
  }

  public Integer getRepeatInHours(Integer forgettingCurveIndex) {
    if (forgettingCurveIndex < DEFAULT_FORGETTING_CURVE_INDEX) {
      return 0;
    }
    int index =
        (forgettingCurveIndex - DEFAULT_FORGETTING_CURVE_INDEX)
            / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    final int remainder = forgettingCurveIndex % DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    final Integer floor = getSpacing(index);
    final Integer ceiling = getSpacing(index + 1);
    return floor * 24
        + (ceiling - floor) * remainder * 24 / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
  }

  private Integer getSpacing(int index) {
    if (index + 1 > spaces.size()) {
      return DEFAULT_SPACES.get(index);
    }
    return spaces.get(index);
  }
}
