package com.odde.doughnut.entities;

public enum SelfEvaluate {
  sad(-1),
  happy(1);

  public final int adjustment;

  SelfEvaluate(int adjustment) {
    this.adjustment = adjustment;
  }
}
