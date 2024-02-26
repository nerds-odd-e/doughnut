package com.odde.doughnut.controllers.json;

import jakarta.validation.constraints.NotNull;

public class ReviewStatus {
  @NotNull public int toRepeatCount;
  @NotNull public int learntCount;
  @NotNull public int notLearntCount;
  @NotNull public int toInitialReviewCount;
}
