package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrentProgressDTO {
  private int carPosition;
  private int roundCount;
  private Integer lastDiceFace;
}
