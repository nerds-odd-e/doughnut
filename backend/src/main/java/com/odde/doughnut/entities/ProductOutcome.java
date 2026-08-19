package com.odde.doughnut.entities;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ProductOutcome {
  GOOD,
  EASY,
  HARD,
  AGAIN,
  CONFUSION;

  static String mappedGradeSqlInList() {
    return Stream.of(GOOD, EASY, HARD, AGAIN)
        .map(outcome -> "'" + outcome.name() + "'")
        .collect(Collectors.joining(", "));
  }
}
