package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RaceGameProgress {
  private Long id;
  private String playerId;
  private int carPosition = 0;
  private int roundCount = 0;
  private Integer lastDiceFace;
  private int carHp = 6;
  private String displayName;
}
