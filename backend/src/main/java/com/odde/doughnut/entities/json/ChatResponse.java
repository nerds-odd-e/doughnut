package com.odde.doughnut.entities.json;

import lombok.Getter;

public class ChatResponse {

  @Getter private final String answer;

  public ChatResponse(String answer) {
    this.answer = answer;
  }
}
