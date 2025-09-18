package com.odde.doughnut.services;

import com.odde.doughnut.entities.Rounds;

public class RacingGameService {
  public RacingGameService() {}

  public Rounds rollDiceSuper(int id) {
    // Mock the latest round
    int dice = (int) (Math.random() * 6) + 1;
    Rounds round = new Rounds();
    round.setPlayer(null);
    round.setGame(null);
    round.setStep(0);
    round.setDamage(0);
    round.setRoundNo(0);
    /// ////////////service
    Rounds currentRound = new Rounds();
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
