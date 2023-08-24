package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.ChatRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/v1")
public class RestChatController {
  @PostMapping("/chat")
  public String chat(ChatRequest request) {
    return "I'm chatGPT";
  }
}
