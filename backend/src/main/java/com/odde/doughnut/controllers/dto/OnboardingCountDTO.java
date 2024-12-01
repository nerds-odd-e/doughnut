package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OnboardingCountDTO {
  private int dueCount;
  private int totalRemainingCount;
}
