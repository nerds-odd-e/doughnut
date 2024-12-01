package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public class RecallStatus {
  @NotNull public int toRepeatCount;
  @NotNull public int learntCount;
}
