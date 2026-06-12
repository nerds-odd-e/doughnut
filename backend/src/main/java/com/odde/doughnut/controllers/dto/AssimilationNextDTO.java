package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationNextDTO {
  private Integer nextNoteId;
  private String nextPropertyKey;
  private AssimilationCountDTO counts;
}
