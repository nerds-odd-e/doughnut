package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IndexControllerTests {

  ApplicationController controller;

  @Test
  void visitWithNoUserSession() {
    controller = new ApplicationController();
    assertEquals("vuejsed", controller.home());
  }
}
