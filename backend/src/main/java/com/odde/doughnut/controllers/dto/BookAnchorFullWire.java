package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"id", "value"})
@Schema(name = "BookAnchor")
public class BookAnchorFullWire {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final int id;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final String value;
}
