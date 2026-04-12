package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

public class BookBlockDepthRequest {

  @NotNull
  @Pattern(regexp = "INDENT|OUTDENT")
  @Getter
  @Setter
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      allowableValues = {"INDENT", "OUTDENT"})
  private String direction;
}
