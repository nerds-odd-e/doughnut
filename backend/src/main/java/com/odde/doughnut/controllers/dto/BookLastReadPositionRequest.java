package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookLastReadPositionRequest {

  @NotNull
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "0-based PDF page index in the viewer")
  private Integer pageIndex;

  @NotNull
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Vertical position within the page in MinerU-normalized space (0-1000)")
  private Integer normalizedY;
}
