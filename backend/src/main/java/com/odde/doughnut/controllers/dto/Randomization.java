package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class Randomization {
  public enum RandomStrategy {
    first,
    last,
    seed,
  }

  public RandomStrategy choose;
  public Integer seed;
}
