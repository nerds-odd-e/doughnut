package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameStatsDTO {
  @Schema(description = "Game ID", example = "1")
  private Integer gameId;

  @Schema(description = "Game name", example = "My Awesome Game")
  private String gameName;

  @Schema(description = "Total number of players currently in the game", example = "3")
  private Integer totalPlayers;

  @Schema(description = "Expected number of players for the game", example = "4")
  private Integer expectedPlayers;

  @Schema(description = "Winning step number", example = "10")
  private Integer winningStep;

  @Schema(description = "Name of the winner", example = "Player One")
  private String winnerName;

  @Schema(description = "Whether the game is complete", example = "false")
  private Boolean isGameComplete;
}
