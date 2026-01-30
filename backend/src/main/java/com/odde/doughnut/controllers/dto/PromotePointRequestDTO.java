package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PromotePointRequestDTO {
  public enum PromotionType {
    CHILD,
    SIBLING
  }

  private String point;

  @NotNull private PromotionType promotionType;
}
