package com.odde.doughnut.algorithms;

public class ClozedString {
  private String cnt;

  public ClozedString(String cnt) {

    this.cnt = cnt;
  }

  @Override
  public String toString() {
    throw new RuntimeException("Not implemented, use `cloze` instead.");
  }

  public String cloze() {
    return cnt;
  }
}
