package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CurrentProgressDTO;
import com.odde.doughnut.controllers.dto.RaceGameProgressDTO;
import com.odde.doughnut.controllers.dto.RaceGameRequestDTO;
import com.odde.doughnut.entities.RaceGameProgress;
import com.odde.doughnut.services.RaceGameService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/race")
public class RaceGameController {

  private final RaceGameService raceGameService;

  public RaceGameController(RaceGameService raceGameService) {
    this.raceGameService = raceGameService;
  }

  @PostMapping("/go_normal")
  public RaceGameProgressDTO rollDice(@RequestBody RaceGameRequestDTO request) {
    RaceGameProgress progress = raceGameService.rollDice(request.getPlayerId());
    return createProgressDTO(progress);
  }

  @GetMapping("/current_progress")
  public RaceGameProgressDTO getCurrentProgress(@RequestParam String playerId) {
    RaceGameProgress progress = raceGameService.getCurrentProgress(playerId);
    return createProgressDTO(progress);
  }

  @PostMapping("/reset")
  public void resetGame(@RequestBody RaceGameRequestDTO request) {
    raceGameService.resetGame(request.getPlayerId());
  }

  private RaceGameProgressDTO createProgressDTO(RaceGameProgress progress) {
    CurrentProgressDTO currentProgress = new CurrentProgressDTO();
    currentProgress.setCarPosition(progress.getCarPosition());
    currentProgress.setRoundCount(progress.getRoundCount());
    currentProgress.setLastDiceFace(progress.getLastDiceFace());

    RaceGameProgressDTO dto = new RaceGameProgressDTO();
    dto.setCurrentProgress(currentProgress);
    return dto;
  }
}
