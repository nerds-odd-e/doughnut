package com.odde.donut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefinedContentResponseDTO {
  private String content;

  public RefinedContentResponseDTO(String content) {
    this.content = content;
  }
}
