package com.odde.doughnut.entities;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ProductOutcome {
  GOOD,
  EASY,
  HARD,
  AGAIN,
  CONFUSION;

  boolean isMappedGrade() {
    return switch (this) {
      case GOOD, EASY, HARD, AGAIN -> true;
      case CONFUSION -> false;
    };
  }

  static String mappedGradeSqlInList() {
    return Stream.of(values())
        .filter(ProductOutcome::isMappedGrade)
        .map(outcome -> "'" + outcome.name() + "'")
        .collect(Collectors.joining(", "));
  }
}
