package com.odde.doughnut.services;

import com.odde.doughnut.entities.Car;
import com.odde.doughnut.entities.RaceGameProgress;
import com.odde.doughnut.entities.Round;
import com.odde.doughnut.repositories.CarRepository;
import com.odde.doughnut.repositories.RoundRepository;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaceGameService {
  private final CarRepository carRepository;
  private final RoundRepository roundRepository;
  private final Random random = new Random();

  public RaceGameService(CarRepository carRepository, RoundRepository roundRepository) {
    this.carRepository = carRepository;
    this.roundRepository = roundRepository;
  }

  @Transactional
  public void rollDice(String playerId) {
    Car car = getOrCreateCar(playerId);

    if (car.getPosition() >= 20) {
      return;
    }

    int diceOutcome = random.nextInt(6) + 1;
    int moveAmount = diceOutcome % 2 == 0 ? 2 : 1;

    car.setPosition(Math.min(20, car.getPosition() + moveAmount));
    car = carRepository.save(car);

    // Create a new round for this dice roll
    Round newRound = new Round();
    newRound.setPlayerId(playerId);
    newRound.setLastDiceFace(diceOutcome);
    // Get the current round count and increment it
    int currentCount = roundRepository.findByPlayerId(playerId).map(Round::getCount).orElse(0);
    newRound.setCount(currentCount + 1);
    roundRepository.save(newRound);
  }

  @Transactional(readOnly = true)
  public RaceGameProgress getCurrentProgress(String playerId) {
    Car car = getOrCreateCar(playerId);
    Round lastRound =
        roundRepository.findByPlayerId(playerId).orElseGet(() -> createNewRound(playerId));
    return createProgress(car, lastRound);
  }

  @Transactional
  public void resetGame(String playerId) {
    Car car = getOrCreateCar(playerId);
    car.setPosition(0);
    carRepository.save(car);

    // Delete all rounds for this player
    roundRepository.deleteByPlayerId(playerId);
  }

  private Car getOrCreateCar(String playerId) {
    return carRepository
        .findByPlayerId(playerId)
        .orElseGet(
            () -> {
              Car newCar = new Car();
              newCar.setPlayerId(playerId);
              return carRepository.save(newCar);
            });
  }

  private Round createNewRound(String playerId) {
    Round newRound = new Round();
    newRound.setPlayerId(playerId);
    return roundRepository.save(newRound);
  }

  private RaceGameProgress createProgress(Car car, Round round) {
    RaceGameProgress progress = new RaceGameProgress();
    progress.setPlayerId(car.getPlayerId());
    progress.setCarPosition(car.getPosition());
    progress.setRoundCount(round.getCount());
    progress.setLastDiceFace(round.getLastDiceFace());
    return progress;
  }
}
