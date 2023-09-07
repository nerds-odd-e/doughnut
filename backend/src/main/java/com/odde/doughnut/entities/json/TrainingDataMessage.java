package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TrainingDataMessage {
  @Setter String role;
  @Setter String content;
}
