package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.RaceGameProgress;
import com.odde.doughnut.repositories.RaceGameProgressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RaceGameServiceTests {

  @Autowired private RaceGameService service;

  @Autowired private RaceGameProgressRepository repository;

  private final String playerId = "test-player-1";

  @Test
  void shouldStartWithInitialPosition() {
    RaceGameProgress progress = service.getCurrentProgress(playerId);
    assertThat(progress.getCarPosition(), equalTo(0));
    assertThat(progress.getRoundCount(), equalTo(0));
    assertThat(progress.getLastDiceFace(), nullValue());
  }

  @Test
  void shouldNotMoveCarBeyondTargetPosition() {
    // Roll dice multiple times to ensure we reach target
    for (int i = 0; i < 15; i++) {
      service.rollDice(playerId);
    }

    RaceGameProgress progress = service.getCurrentProgress(playerId);
    assertThat(progress.getCarPosition(), lessThanOrEqualTo(20));
  }

  @Test
  void shouldPersistGameState() {
    service.rollDice(playerId);

    RaceGameProgress progress = repository.findByPlayerId(playerId).orElseThrow();
    assertThat(progress.getRoundCount(), equalTo(1));
    assertThat(progress.getCarPosition(), greaterThan(0));
  }

  @Test
  void shouldMoveCarBasedOnDiceOutcome() {
    RaceGameProgress progress = service.rollDice(playerId);

    // Verify dice outcome is between 1 and 6
    assertThat(progress.getLastDiceFace(), allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(6)));

    // Verify car position based on dice outcome
    if (progress.getLastDiceFace() % 2 == 0) {
      assertThat(progress.getCarPosition(), equalTo(2)); // Even number moves 2 positions
    } else {
      assertThat(progress.getCarPosition(), equalTo(1)); // Odd number moves 1 position
    }
  }

  @Test
  void shouldResetGameState() {
    // First move the car
    service.rollDice(playerId);

    // Then reset the game
    service.resetGame(playerId);

    // Verify game is reset
    RaceGameProgress progress = service.getCurrentProgress(playerId);
    assertThat(progress.getCarPosition(), equalTo(0));
    assertThat(progress.getRoundCount(), equalTo(0));
    assertThat(progress.getLastDiceFace(), nullValue());
  }

  @Test
  void shouldNotMoveCarWhenAlreadyAtTarget() {
    // First get to target position
    while (service.getCurrentProgress(playerId).getCarPosition() < 20) {
      service.rollDice(playerId);
    }

    // Record the state
    RaceGameProgress beforeRoll = service.getCurrentProgress(playerId);

    // Try to roll again
    RaceGameProgress afterRoll = service.rollDice(playerId);

    // Verify nothing changed
    assertThat(afterRoll.getCarPosition(), equalTo(beforeRoll.getCarPosition()));
    assertThat(afterRoll.getRoundCount(), equalTo(beforeRoll.getRoundCount()));
  }
}
