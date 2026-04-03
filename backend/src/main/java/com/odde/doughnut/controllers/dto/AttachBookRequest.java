package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachBookRequest {

  @NotBlank
  @Size(max = 512)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String bookName;

  @NotBlank
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String format;

  @NotNull
  @Valid
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private AttachBookLayoutRequest layout;
}
