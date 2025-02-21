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
  public void rollDiceNormal(@RequestBody RaceGameRequestDTO request) {
    raceGameService.rollDiceNormal(request.getPlayerId());
  }

  @PostMapping("/go_super")
  public void rollDiceSuper(@RequestBody RaceGameRequestDTO request) {
    raceGameService.rollDiceSuper(request.getPlayerId());
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
    currentProgress.setCarHp(progress.getCarHp());
    currentProgress.setDisplayName(progress.getDisplayName());

    RaceGameProgressDTO dto = new RaceGameProgressDTO();
    dto.setCurrentProgress(currentProgress);
    return dto;
  }
}
