package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationCountDTO {
  private int dueCount;
  private int totalRemainingCount;
}
