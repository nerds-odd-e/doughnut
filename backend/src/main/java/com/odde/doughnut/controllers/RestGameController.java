package com.odde.doughnut.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odde.doughnut.entities.Games;
import com.odde.doughnut.entities.Players;
import com.odde.doughnut.entities.Rounds;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.RacingGameService;
import java.util.List;
import java.util.stream.StreamSupport;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class RestGameController {
  private final ModelFactoryService modelFactoryService;
  private final RacingGameService racingGameService = new RacingGameService();

  public RestGameController(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping("/join")
  public Players joinGame() {
    Games game = new Games();
    game.setNumberOfPlayers(4);
    //      game.setCreatedAt(LocalDateTime.now());
    game.setUpdatedDate(new Date());
    game.setEndDate(new Date());
    game.setMaxSteps(6);
    modelFactoryService.gamesRepository.save(game);
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
    Iterable<Players> playersList = modelFactoryService.playersRepository.findAll();
    return StreamSupport.stream(playersList.spliterator(), false).toList();
  }

  @PostMapping("/dice/{id}")
  public Rounds rollDice(@RequestParam("id") Integer id, @RequestParam String mode) {
    if (mode.equals("SUPER")) {
      return racingGameService.rollDiceSuper(id);
    }
    return rollDiceNormal(id);
  }

  public Rounds rollDiceNormal(int id) {
    // Implementation for joining a game
    Rounds round = new Rounds();
    Optional<Players> player = modelFactoryService.playersRepository.findById(id);
    if (player.isPresent()) {
      player.get().getRounds().add(round);
      int dice = (int) (Math.random() * 6) + 1;
      round.setRoundNo(player.get().getRounds().size() + 1);
      round.setDamage(0);
      round.setDice(dice);
      round.setUpdateDate(new Date());
      round.setCreateDate(new Date());
      int sum =
          player.get().getRounds().stream()
              .mapToInt(r -> r.getDice() % 2 == 1 ? 1 : 2) // odd: +1, even: +2
              .sum();

      round.setStep(sum);
      modelFactoryService.playersRepository.save(player.get());
    }
    return round;
  }
}
