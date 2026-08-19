package com.odde.doughnut.entities;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ProductOutcome {
  GOOD,
  EASY,
  HARD,
  SHRINK,
  AGAIN,
  AGAIN_ZERO,
  CONFUSION;

  static String mappedGradeSqlInList() {
    return Arrays.stream(values())
        .filter(outcome -> outcome != CONFUSION)
        .map(outcome -> "'" + outcome.name() + "'")
        .collect(Collectors.joining(", "));
  }
}
