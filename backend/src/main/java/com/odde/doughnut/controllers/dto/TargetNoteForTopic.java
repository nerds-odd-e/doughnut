package com.odde.doughnut.controllers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class TargetNoteForTopic {
  private Integer id;
  private String topicConstructor;
  private TargetNoteForTopic targetNoteForTopic;
}
