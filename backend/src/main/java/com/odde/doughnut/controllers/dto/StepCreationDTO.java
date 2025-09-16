package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepCreationDTO {
  @Schema(description = "Move description", example = "Attack with sword")
  private String move;

  @Schema(description = "Damage dealt", example = "15")
  @Positive(message = "Damage must be positive")
  private Integer damage;

  @NotNull(message = "Current step is required")
  @Positive(message = "Current step must be positive")
  @Schema(description = "Current step number", example = "5")
  private Integer currentStep;

  @NotNull(message = "Player ID is required")
  @Positive(message = "Player ID must be positive")
  @Schema(description = "ID of the player making this step", example = "1")
  private Integer playerId;
}
