package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerCreationDTO {
  @NotBlank(message = "Player name is required")
  @Schema(description = "Name of the player", example = "Player One")
  private String name;

  @NotNull(message = "Game ID is required")
  @Positive(message = "Game ID must be positive")
  @Schema(description = "ID of the game this player belongs to", example = "1")
  private Integer gameId;
}
