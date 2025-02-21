package com.odde.doughnut.services;

import com.odde.doughnut.entities.Car;
import com.odde.doughnut.entities.RaceGameProgress;
import com.odde.doughnut.entities.Round;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.repositories.CarRepository;
import com.odde.doughnut.repositories.RoundRepository;
import java.util.function.IntUnaryOperator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaceGameService {
  private final CarRepository carRepository;
  private final RoundRepository roundRepository;
  private final Randomizer random = new RealRandomizer();

  private final int maxHp = 6;

  public RaceGameService(CarRepository carRepository, RoundRepository roundRepository) {
    this.carRepository = carRepository;
    this.roundRepository = roundRepository;
  }

  @Transactional
  public void rollDiceNormal(String playerId) {
    rollDice(playerId, (dice) -> dice % 2 == 0 ? 2 : 1, false);
  }

  @Transactional
  public void rollDiceSuper(String playerId) {
    rollDice(playerId, (dice) -> dice, true);
  }

  private void rollDice(String playerId, IntUnaryOperator moveCalculator, boolean reduceHp) {
    Car car = getOrCreateCar(playerId);

    if (car.getPosition() >= 20) {
      return;
    }

    int diceOutcome = random.randomInteger(1, 6);
    int damage = maxHp - car.getHp();
    int moveAmount = Math.max(0, moveCalculator.applyAsInt(diceOutcome) - damage);

    car.setPosition(Math.min(20, car.getPosition() + moveAmount));
    if (reduceHp) {
      car.setHp(Math.max(0, car.getHp() - 1));
    }
    carRepository.save(car);

    // Create a new round for this dice roll
    Round newRound = new Round();
    newRound.setPlayerId(playerId);
    newRound.setLastDiceFace(diceOutcome);
    int currentRoundCount = roundRepository.findByPlayerId(playerId).map(Round::getCount).orElse(0);
    newRound.setCount(currentRoundCount + 1);
    roundRepository.save(newRound);
  }

  @Transactional
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
    car.setHp(6);
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
    progress.setCarHp(car.getHp());
    progress.setDisplayName(car.getDisplayName());
    return progress;
  }
}
