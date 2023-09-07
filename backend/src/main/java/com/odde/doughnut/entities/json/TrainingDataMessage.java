package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

public class TrainingDataMessage {
  @Setter @Getter String role;
  @Setter @Getter String content;

  public TrainingDataMessage() {}

  public TrainingDataMessage(String role, String content) {
    this.role = role;
    this.content = content;
  }
}
