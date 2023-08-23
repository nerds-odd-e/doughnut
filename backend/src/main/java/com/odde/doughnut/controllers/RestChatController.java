package com.odde.doughnut.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/v1")
public class RestChatController {
  @PostMapping("/chat")
  public String getChat(String askStatement) {
    return "I'm chatGPT";
  }
}
