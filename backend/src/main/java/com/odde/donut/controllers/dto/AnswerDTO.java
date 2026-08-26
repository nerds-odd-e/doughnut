package com.odde.donut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class AnswerDTO {
  @Getter @Setter private Integer choiceIndex;
  @Getter @Setter private Integer thinkingTimeMs;
}
