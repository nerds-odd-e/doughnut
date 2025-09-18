package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Players;
import com.odde.doughnut.entities.Rounds;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class RestGameController {
  private final ModelFactoryService modelFactoryService;

  public RestGameController(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping("/join")
  public Players joinGame() {
    // Implementation for joining a game
    String playerName =
        "Player-"
            + System.currentTimeMillis(); // This should come from the request in a real scenario
    Players player = new Players();
    player.setName(playerName);
    modelFactoryService.playersRepository.save(player);
    return player;
  }

  @PostMapping("/dice/{id}")
  public Rounds rollDice(@RequestParam int id) {
    // Implementation for joining a game
    int dice = 5;
    Rounds round = new Rounds();
    round.setStep(1);
    round.setDamage(0);
    round.setDice(5);
    return round;
  }
}
