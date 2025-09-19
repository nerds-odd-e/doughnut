package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Players;
import com.odde.doughnut.entities.Rounds;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestGameControllerTest {
  @Autowired RestGameController restGameController;
  @Autowired MakeMe makeMe;

  @Test
  void testJoinGame_Case1() {
    Players player = restGameController.joinGame();
    assertNotNull(player);
  }

  @Test
  void testDice() {
    Players player = restGameController.joinGame();
    assertNotNull(player);
    Rounds round = restGameController.rollDice(player.getId(), "NORMAL");
    assertNotNull(round);
    // just check if the dice is in range 1-6
    assertTrue(round.getDice() >= 1 && round.getDice() <= 6);
  }

  @Test
  void fetchPlayers_case_multiplayers_join() {
    Players player1 = makeMe.aPlayer().please();
    Players player2 = makeMe.aPlayer().please();
    List<Players> playersList = restGameController.fetchPlayers();
    assertEquals(2, playersList.size());
  }
}
