package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookBlockReadingRecordPutRequest {

  @NotNull
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      allowableValues = {"READ", "SKIMMED"},
      description = "Reading disposition for the book block's direct content")
  private String status;
}
