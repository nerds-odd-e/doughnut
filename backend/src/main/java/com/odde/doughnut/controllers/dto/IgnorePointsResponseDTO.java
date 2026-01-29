package com.odde.doughnut.controllers.dto;

import lombok.Getter;

public class IgnorePointsResponseDTO {
  @Getter private final boolean success;

  public IgnorePointsResponseDTO(boolean success) {
    this.success = success;
  }
}
