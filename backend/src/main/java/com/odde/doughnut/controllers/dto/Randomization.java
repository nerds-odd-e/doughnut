package com.odde.doughnut.controllers.dto;

public class Randomization {
  public enum RandomStrategy {
    first,
    last,
    seed,
  }

  public RandomStrategy choose;
  public Integer seed;
}
