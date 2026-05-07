package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RemovePointsResponseDTO {
  private String content;

  public RemovePointsResponseDTO(String content) {
    this.content = content;
  }
}
