package com.odde.doughnut.services;

import com.odde.doughnut.entities.RaceGameProgress;
import com.odde.doughnut.repositories.RaceGameProgressRepository;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaceGameService {
  private final RaceGameProgressRepository repository;
  private final Random random = new Random();

  public RaceGameService(RaceGameProgressRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public RaceGameProgress rollDice(String playerId) {
    RaceGameProgress progress = getOrCreateProgress(playerId);

    if (progress.getCarPosition() >= 20) {
      return progress;
    }

    int diceOutcome = random.nextInt(6) + 1;
    int moveAmount = diceOutcome % 2 == 0 ? 2 : 1;

    progress.setCarPosition(Math.min(20, progress.getCarPosition() + moveAmount));
    progress.setLastDiceFace(diceOutcome);
    progress.setRoundCount(progress.getRoundCount() + 1);

    return repository.save(progress);
  }

  @Transactional(readOnly = true)
  public RaceGameProgress getCurrentProgress(String playerId) {
    return getOrCreateProgress(playerId);
  }

  @Transactional
  public void resetGame(String playerId) {
    RaceGameProgress progress = getOrCreateProgress(playerId);
    progress.setCarPosition(0);
    progress.setRoundCount(0);
    progress.setLastDiceFace(null);
    repository.save(progress);
  }

  private RaceGameProgress getOrCreateProgress(String playerId) {
    return repository
        .findByPlayerId(playerId)
        .orElseGet(
            () -> {
              RaceGameProgress newProgress = new RaceGameProgress();
              newProgress.setPlayerId(playerId);
              return repository.save(newProgress);
            });
  }
}
