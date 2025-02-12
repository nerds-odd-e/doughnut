package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class SpellingQuestion {
  @Getter @Setter private String stem;

  public SpellingQuestion(String stem) {
    this.stem = stem;
  }
}
