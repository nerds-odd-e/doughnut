package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameUpdateDTO {
  @Schema(description = "Name of the game", example = "Updated Game Name")
  private String name;

  @Schema(description = "Number of players in the game", example = "6")
  @Positive(message = "Number of players must be positive")
  private Integer noPlayers;

  @Schema(description = "Winning step number", example = "10")
  @Positive(message = "Winning step must be positive")
  private Integer winningStep;

  @Schema(description = "Name of the winner", example = "Player One")
  private String nameOfWinner;
}
