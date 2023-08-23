package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestChatControllerTests {
  RestChatController controller;

  @BeforeEach
  void setUp() {
    controller = new RestChatController();
  }

  @Test
  void chatWithAI() {
    // then: I want to get json response from openai
    String res = controller.getChat("What your name?");
    assertEquals("I'm chatGPT", res);
  }
}
