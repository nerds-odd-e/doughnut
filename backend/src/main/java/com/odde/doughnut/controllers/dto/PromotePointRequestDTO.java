package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PromotePointRequestDTO {
  private String point;

  @NotNull private Integer parentNoteId;
}
