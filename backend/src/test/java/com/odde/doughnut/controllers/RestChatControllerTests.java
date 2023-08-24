package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    ChatRequest request = new ChatRequest();
    String res = controller.chat(request);
    assertEquals("I'm chatGPT", res);
  }
}
