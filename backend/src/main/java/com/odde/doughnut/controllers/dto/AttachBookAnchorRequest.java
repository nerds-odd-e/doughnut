package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachBookAnchorRequest {

  @NotBlank
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String anchorFormat;

  @NotBlank
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String value;
}
