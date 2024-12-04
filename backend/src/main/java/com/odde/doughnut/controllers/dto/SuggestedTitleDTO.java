package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SuggestedTitleDTO {
  private String title;

  public SuggestedTitleDTO(String title) {
    this.title = title;
  }
}
