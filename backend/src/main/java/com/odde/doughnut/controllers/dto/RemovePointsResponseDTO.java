package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RemovePointsResponseDTO {
  private String details;

  public RemovePointsResponseDTO(String details) {
    this.details = details;
  }
}
