package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Players;
import com.odde.doughnut.entities.Rounds;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
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

  @GetMapping("/fetch")
  public List<Players> fetchPlayers() {
    //    Iterable<Players> playersList = modelFactoryService.playersRepository.findAll();
    //    return StreamSupport.stream(playersList.spliterator(), false).toList();
    Players player1 = new Players();
    player1.setName("Player1");
    Players player2 = new Players();
    player2.setName("Player2");
    List<Players> playersList = List.of(player1, player2);
    return playersList;
  }

  @PostMapping("/dice/{id}")
  public Rounds rollDice(@RequestParam int id) {
    // Implementation for joining a game
    int dice = (int) (Math.random() * 6) + 1;
    Rounds round = new Rounds();
    round.setStep(1);
    round.setDamage(0);
    round.setDice(dice);
    return round;
  }
}
