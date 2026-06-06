package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Rename a property memory tracker's frontmatter key in place.")
@Getter
@Setter
public class UpdateMemoryTrackerPropertyKeyDTO {

  @NotBlank
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "New frontmatter property key for this tracker")
  private String propertyKey;
}
