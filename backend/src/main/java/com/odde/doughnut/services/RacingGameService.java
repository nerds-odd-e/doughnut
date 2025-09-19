package com.odde.doughnut.services;

import com.odde.doughnut.entities.Games;
import com.odde.doughnut.entities.Rounds;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Comparator;
import java.util.Objects;

public class RacingGameService {
  private final ModelFactoryService modelFactoryService;

  public RacingGameService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public Rounds rollDiceSuper(int id) {
    int dice = (int) (Math.random() * 6) + 1;
    var player = modelFactoryService.playersRepository.findById(id).orElse(null);
    Rounds round =
        player.getRounds().stream()
            .filter(Objects::nonNull)
            .filter(r -> r.getUpdateDate() != null)
            .max(Comparator.comparing(Rounds::getUpdateDate))
            .orElse(null);

    Rounds currentRound = new Rounds();
    if (round == null) {
      round = new Rounds();
      round.setPlayer(player);
      round.setGame(new Games());
      round.setStep(0);
      round.setDamage(0);
      round.setRoundNo(0);
    }
    int currentDamage = round.getDamage() + 1;
    currentRound.setDamage(currentDamage);
    currentRound.setDice(dice);
    currentRound.setMode("SUPER");
    currentRound.setStep(round.getStep() + dice - round.getDamage());
    currentRound.setPlayer(round.getPlayer());
    currentRound.setRoundNo(round.getRoundNo() + 1);
    currentRound.setGame(round.getGame());

    return currentRound;
  }
}
