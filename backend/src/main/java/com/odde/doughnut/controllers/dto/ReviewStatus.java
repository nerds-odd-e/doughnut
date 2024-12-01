package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public class ReviewStatus {
  @NotNull public int toRepeatCount;
  @NotNull public int learntCount;
  @NotNull public int notLearntCount;
}
