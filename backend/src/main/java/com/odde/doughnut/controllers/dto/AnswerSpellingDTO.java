package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class AnswerSpellingDTO {
  @Getter @Setter private String spellingAnswer;
  @Getter @Setter private Integer thinkingTimeMs;
}
