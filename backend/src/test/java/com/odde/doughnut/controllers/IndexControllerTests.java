package com.odde.doughnut.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexControllerTests {

  ApplicationController controller;

  @Test
  void visitWithNoUserSession() {
    controller = new ApplicationController();
    assertEquals("vuejsed", controller.home());
  }
}
