package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Game;
import com.odde.doughnut.entities.Player;
import com.odde.doughnut.entities.Step;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Game Management", description = "APIs for managing games, players, and steps")
public class RestGameController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  public RestGameController(ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  // Game CRUD Operations

  @PostMapping("")
  @Transactional
  @Operation(
      summary = "Create a new game",
      description = "Creates a new game with the specified name and number of players")
  public ResponseEntity<Game> createGame(@Valid @RequestBody GameCreationDTO gameCreation) {
    currentUser.assertLoggedIn();

    Game game = new Game();
    game.setName(gameCreation.getName());
    game.setNoPlayers(gameCreation.getNoPlayers());

    Game savedGame = modelFactoryService.save(game);
    return ResponseEntity.status(HttpStatus.CREATED).body(savedGame);
  }

  @GetMapping("")
  @Operation(summary = "Get all games", description = "Retrieves all games")
  public ResponseEntity<List<Game>> getAllGames() {
    currentUser.assertLoggedIn();

    List<Game> games = (List<Game>) modelFactoryService.gameRepository.findAll();
    return ResponseEntity.ok(games);
  }

  @GetMapping("/{gameId}")
  @Operation(summary = "Get game by ID", description = "Retrieves a specific game by its ID")
  public ResponseEntity<Game> getGame(
      @PathVariable @Parameter(description = "Game ID") @Schema(type = "integer") Integer gameId) {
    currentUser.assertLoggedIn();

    Optional<Game> game = modelFactoryService.gameRepository.findById(gameId);
    return game.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{gameId}")
  @Transactional
  @Operation(summary = "Update game", description = "Updates an existing game")
  public ResponseEntity<Game> updateGame(
      @PathVariable @Parameter(description = "Game ID") @Schema(type = "integer") Integer gameId,
      @Valid @RequestBody GameUpdateDTO gameUpdate) {
    currentUser.assertLoggedIn();

    Optional<Game> gameOpt = modelFactoryService.gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Game game = gameOpt.get();
    if (gameUpdate.getName() != null) {
      game.setName(gameUpdate.getName());
    }
    if (gameUpdate.getNoPlayers() != null) {
      game.setNoPlayers(gameUpdate.getNoPlayers());
    }
    if (gameUpdate.getWinningStep() != null) {
      game.setWinningStep(gameUpdate.getWinningStep());
    }
    if (gameUpdate.getNameOfWinner() != null) {
      game.setNameOfWinner(gameUpdate.getNameOfWinner());
    }

    Game updatedGame = modelFactoryService.save(game);
    return ResponseEntity.ok(updatedGame);
  }

  @DeleteMapping("/{gameId}")
  @Transactional
  @Operation(
      summary = "Delete game",
      description = "Deletes a game and all associated players and steps")
  public ResponseEntity<Void> deleteGame(
      @PathVariable @Parameter(description = "Game ID") @Schema(type = "integer") Integer gameId) {
    currentUser.assertLoggedIn();

    Optional<Game> gameOpt = modelFactoryService.gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Delete associated players and steps first
    List<Player> players = modelFactoryService.playerRepository.findByIdOfGame(gameId);
    for (Player player : players) {
      List<Step> steps = modelFactoryService.stepRepository.findByPlayerId(player.getId());
      for (Step step : steps) {
        modelFactoryService.remove(step);
      }
      modelFactoryService.remove(player);
    }

    modelFactoryService.remove(gameOpt.get());
    return ResponseEntity.noContent().build();
  }

  // Player Operations

  @PostMapping("/{gameId}/players")
  @Transactional
  @Operation(summary = "Add player to game", description = "Adds a new player to an existing game")
  public ResponseEntity<Player> addPlayer(
      @PathVariable @Parameter(description = "Game ID") @Schema(type = "integer") Integer gameId,
      @Valid @RequestBody PlayerCreationDTO playerCreation) {
    currentUser.assertLoggedIn();

    Optional<Game> gameOpt = modelFactoryService.gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Player player = new Player(playerCreation.getName(), gameId);
    Player savedPlayer = modelFactoryService.save(player);
    return ResponseEntity.status(HttpStatus.CREATED).body(savedPlayer);
  }

  @GetMapping("/{gameId}/players")
  @Operation(
      summary = "Get players in game",
      description = "Retrieves all players in a specific game")
  public ResponseEntity<List<Player>> getPlayersInGame(
      @PathVariable @Parameter(description = "Game ID") @Schema(type = "integer") Integer gameId) {
    currentUser.assertLoggedIn();

    List<Player> players = modelFactoryService.playerRepository.findByIdOfGame(gameId);
    return ResponseEntity.ok(players);
  }

  @GetMapping("/players/{playerId}")
  @Operation(summary = "Get player by ID", description = "Retrieves a specific player by ID")
  public ResponseEntity<Player> getPlayer(
      @PathVariable @Parameter(description = "Player ID") @Schema(type = "integer")
          Integer playerId) {
    currentUser.assertLoggedIn();

    Optional<Player> player = modelFactoryService.playerRepository.findById(playerId);
    return player.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/players/{playerId}")
  @Transactional
  @Operation(summary = "Delete player", description = "Deletes a player and all their steps")
  public ResponseEntity<Void> deletePlayer(
      @PathVariable @Parameter(description = "Player ID") @Schema(type = "integer")
          Integer playerId) {
    currentUser.assertLoggedIn();

    Optional<Player> playerOpt = modelFactoryService.playerRepository.findById(playerId);
    if (playerOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Delete associated steps first
    List<Step> steps = modelFactoryService.stepRepository.findByPlayerId(playerId);
    for (Step step : steps) {
      modelFactoryService.remove(step);
    }

    modelFactoryService.remove(playerOpt.get());
    return ResponseEntity.noContent().build();
  }

  // Step Operations

  @PostMapping("/players/{playerId}/steps")
  @Transactional
  @Operation(summary = "Add step for player", description = "Adds a new step for a specific player")
  public ResponseEntity<Step> addStep(
      @PathVariable @Parameter(description = "Player ID") @Schema(type = "integer")
          Integer playerId,
      @Valid @RequestBody StepCreationDTO stepCreation) {
    currentUser.assertLoggedIn();

    Optional<Player> playerOpt = modelFactoryService.playerRepository.findById(playerId);
    if (playerOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Step step =
        new Step(
            stepCreation.getMove(),
            stepCreation.getDamage(),
            stepCreation.getCurrentStep(),
            playerId);
    Step savedStep = modelFactoryService.save(step);
    return ResponseEntity.status(HttpStatus.CREATED).body(savedStep);
  }

  @GetMapping("/players/{playerId}/steps")
  @Operation(
      summary = "Get steps for player",
      description = "Retrieves all steps for a specific player")
  public ResponseEntity<List<Step>> getStepsForPlayer(
      @PathVariable @Parameter(description = "Player ID") @Schema(type = "integer")
          Integer playerId) {
    currentUser.assertLoggedIn();

    List<Step> steps = modelFactoryService.stepRepository.findByPlayerId(playerId);
    return ResponseEntity.ok(steps);
  }

  @GetMapping("/steps/{stepId}")
  @Operation(summary = "Get step by ID", description = "Retrieves a specific step by ID")
  public ResponseEntity<Step> getStep(
      @PathVariable @Parameter(description = "Step ID") @Schema(type = "integer") Integer stepId) {
    currentUser.assertLoggedIn();

    Optional<Step> step = modelFactoryService.stepRepository.findById(stepId);
    return step.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/steps/{stepId}")
  @Transactional
  @Operation(summary = "Delete step", description = "Deletes a specific step")
  public ResponseEntity<Void> deleteStep(
      @PathVariable @Parameter(description = "Step ID") @Schema(type = "integer") Integer stepId) {
    currentUser.assertLoggedIn();

    Optional<Step> stepOpt = modelFactoryService.stepRepository.findById(stepId);
    if (stepOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    modelFactoryService.remove(stepOpt.get());
    return ResponseEntity.noContent().build();
  }

  // Game Statistics

  @GetMapping("/{gameId}/stats")
  @Operation(
      summary = "Get game statistics",
      description = "Retrieves statistics for a specific game")
  public ResponseEntity<GameStatsDTO> getGameStats(
      @PathVariable @Parameter(description = "Game ID") @Schema(type = "integer") Integer gameId) {
    currentUser.assertLoggedIn();

    Optional<Game> gameOpt = modelFactoryService.gameRepository.findById(gameId);
    if (gameOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Game game = gameOpt.get();
    List<Player> players = modelFactoryService.playerRepository.findByIdOfGame(gameId);

    GameStatsDTO stats = new GameStatsDTO();
    stats.setGameId(gameId);
    stats.setGameName(game.getName());
    stats.setTotalPlayers(players.size());
    stats.setExpectedPlayers(game.getNoPlayers());
    stats.setWinningStep(game.getWinningStep());
    stats.setWinnerName(game.getNameOfWinner());
    stats.setIsGameComplete(game.getWinningStep() != null && game.getNameOfWinner() != null);

    return ResponseEntity.ok(stats);
  }
}
