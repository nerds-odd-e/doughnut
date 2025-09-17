package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

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

  @Test
  void testJoinGame_Case1() {
    int playerId = restGameController.joinGame();
    assertEquals(1, playerId);
  }

  @Test
  void testJoinGame() {
    int playerId = restGameController.joinGame();
    assertEquals(1, playerId);
  }
}
