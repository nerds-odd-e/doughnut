package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.RaceGameProgressDTO;
import com.odde.doughnut.controllers.dto.RaceGameRequestDTO;
import com.odde.doughnut.repositories.RaceGameProgressRepository;
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

  @Autowired private RaceGameProgressRepository repository;

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

    RaceGameProgressDTO progress = controller.rollDice(request);

    assertThat(progress.getCurrentProgress().getCarPosition(), greaterThan(0));
    assertThat(progress.getCurrentProgress().getRoundCount(), equalTo(1));
    assertThat(progress.getCurrentProgress().getLastDiceFace(), notNullValue());
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

  @Test
  void shouldReturnCorrectDTOFormat() {
    RaceGameRequestDTO request = new RaceGameRequestDTO();
    request.setPlayerId(playerId);

    RaceGameProgressDTO response = controller.rollDice(request);

    assertThat(response.getCurrentProgress(), notNullValue());
    assertThat(response.getCurrentProgress().getCarPosition(), greaterThanOrEqualTo(0));
    assertThat(response.getCurrentProgress().getRoundCount(), equalTo(1));
    assertThat(
        response.getCurrentProgress().getLastDiceFace(),
        allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(6)));
  }
}
