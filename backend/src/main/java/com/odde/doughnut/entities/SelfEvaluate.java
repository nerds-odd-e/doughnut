package com.odde.doughnut.entities;

public enum SelfEvaluate {
  reset(-2),
  satisfying(0),
  sad(-1),
  happy(1);

  public final int adjustment;

  SelfEvaluate(int adjustment) {
    this.adjustment = adjustment;
  }
}
