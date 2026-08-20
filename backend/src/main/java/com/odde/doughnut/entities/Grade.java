package com.odde.doughnut.entities;

/** FSRS scheduling evaluation. Numeric value is FSRS {@code G}. */
public enum Grade {
  AGAIN(1),
  HARD(2),
  GOOD(3),
  EASY(4);

  private final int value;

  Grade(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  /** Identity from FSRS {@code G} (1–4), not a Tutor “score” translation. */
  public static Grade fromValue(int value) {
    return switch (value) {
      case 1 -> AGAIN;
      case 2 -> HARD;
      case 3 -> GOOD;
      case 4 -> EASY;
      default -> throw new IllegalArgumentException("Grade G must be 1–4: " + value);
    };
  }
}
