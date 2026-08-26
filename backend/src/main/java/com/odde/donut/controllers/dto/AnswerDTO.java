package com.odde.donut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class AnswerDTO {
  @Getter @Setter private Integer choiceIndex;
  @Getter @Setter private Integer thinkingTimeMs;
  @Getter @Setter private Integer awayMs;
  @Getter @Setter private Integer awayCount;
  @Getter @Setter private Integer detourMs;
  @Getter @Setter private Integer detourCount;
  @Getter @Setter private Integer idleMs;
}
