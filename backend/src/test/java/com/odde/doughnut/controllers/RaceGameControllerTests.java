package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.RaceGameProgressDTO;
import com.odde.doughnut.controllers.dto.RaceGameRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RaceGameControllerTests {

  @Autowired private RaceGameController controller;

  private final String playerId = "test-player-1";

  @Test
  void shouldCreateNewProgressForNewPlayer() {
    RaceGameProgressDTO progress = controller.getCurrentProgress(playerId);
    assertThat(progress.getCurrentProgress().getCarPosition(), equalTo(0));
    assertThat(progress.getCurrentProgress().getRoundCount(), equalTo(0));
    assertThat(progress.getCurrentProgress().getLastDiceFace(), nullValue());
  }

  @Test
  void shouldMoveCarOnDiceRoll() {
    RaceGameRequestDTO request = new RaceGameRequestDTO();
    request.setPlayerId(playerId);

    controller.rollDice(request);
    RaceGameProgressDTO progress = controller.getCurrentProgress(playerId);

    assertThat(
        progress.getCurrentProgress().getLastDiceFace(),
        allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(6)));
    assertThat(progress.getCurrentProgress().getCarPosition(), greaterThan(0));
    assertThat(progress.getCurrentProgress().getRoundCount(), equalTo(1));
  }

  @Test
  void shouldResetGameState() {
    // First make some progress
    RaceGameRequestDTO request = new RaceGameRequestDTO();
    request.setPlayerId(playerId);
    controller.rollDice(request);

    // Then reset
    controller.resetGame(request);

    RaceGameProgressDTO progress = controller.getCurrentProgress(playerId);
    assertThat(progress.getCurrentProgress().getCarPosition(), equalTo(0));
    assertThat(progress.getCurrentProgress().getRoundCount(), equalTo(0));
    assertThat(progress.getCurrentProgress().getLastDiceFace(), nullValue());
  }
}
