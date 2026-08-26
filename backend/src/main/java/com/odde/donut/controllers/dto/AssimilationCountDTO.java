package com.odde.donut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationCountDTO {
  private int dueCount;
  private int assimilatedCountOfTheDay;
  private int totalUnassimilatedCount;
}
