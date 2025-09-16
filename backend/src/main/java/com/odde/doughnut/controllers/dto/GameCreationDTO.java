package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameCreationDTO {
  @NotBlank(message = "Game name is required")
  @Schema(description = "Name of the game", example = "My Awesome Game")
  private String name;

  @NotNull(message = "Number of players is required")
  @Positive(message = "Number of players must be positive")
  @Schema(description = "Number of players in the game", example = "4")
  private Integer noPlayers;
}
