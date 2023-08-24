package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.ChatRequest;
import com.odde.doughnut.entities.json.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/v1")
public class RestChatController {
  @PostMapping("/chat")
  public ChatResponse chat(ChatRequest request) {

    return new ChatResponse("I'm ChatGPT");
  }
}
