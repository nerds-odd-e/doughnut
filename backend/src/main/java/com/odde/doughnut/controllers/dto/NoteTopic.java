package com.odde.doughnut.controllers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
@Data
public class NoteTopic {
  @NonNull private Integer id;
  @NonNull private String topicConstructor;
  private NoteTopic targetNoteTopic;
}
