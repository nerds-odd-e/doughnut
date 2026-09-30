package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "A property's single value became one value of a list: its trackers follow that value.")
@Getter
@Setter
public class FollowPropertyValueDTO {

  @NotBlank
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Frontmatter property key")
  private String propertyKey;

  @NotBlank
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The list value that was the property's single value")
  private String propertyValue;
}
