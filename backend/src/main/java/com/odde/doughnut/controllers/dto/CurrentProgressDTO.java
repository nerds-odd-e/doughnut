package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrentProgressDTO {
  private int carPosition;
  private int carHp;
  private int roundCount;
  private int lastDiceFace;
  private String displayName;
}
