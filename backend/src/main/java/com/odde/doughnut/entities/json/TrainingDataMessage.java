package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TrainingDataMessage {
  private String role;
  private String content;
}
