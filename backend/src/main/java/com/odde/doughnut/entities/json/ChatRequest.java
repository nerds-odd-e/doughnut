package com.odde.doughnut.entities.json;

import lombok.Getter;

public class ChatRequest {

  @Getter private final String ask;

  public ChatRequest(String ask) {
    this.ask = ask;
  }
}
