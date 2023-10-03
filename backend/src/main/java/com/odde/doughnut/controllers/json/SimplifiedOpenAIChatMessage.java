package com.odde.doughnut.controllers.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SimplifiedOpenAIChatMessage {
  private String role;
  private String content;
}
