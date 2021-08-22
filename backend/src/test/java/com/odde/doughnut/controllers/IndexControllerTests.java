package com.odde.doughnut.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexControllerTests {

  IndexController controller;

  @Test
  void visitWithNoUserSession() {
    controller = new IndexController();
    assertEquals("vuejsed", controller.home());
  }
}
