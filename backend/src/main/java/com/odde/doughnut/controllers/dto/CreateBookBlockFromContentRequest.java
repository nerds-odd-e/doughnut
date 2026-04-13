package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class CreateBookBlockFromContentRequest {

  @NotNull
  @Getter
  @Setter
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "Split the owning book block at this imported content row; that row and following rows become a new child block.")
  private Integer fromBookContentBlockId;
}
