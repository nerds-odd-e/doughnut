package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IndexControllerTests {
  @Autowired ApplicationController controller;

  @Test
  void visitWithNoUserSession() {
    assertEquals("/index.html", controller.home());
  }
}
