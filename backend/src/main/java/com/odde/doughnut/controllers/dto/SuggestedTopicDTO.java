package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SuggestedTopicDTO {
  private String topic;

  public SuggestedTopicDTO(String topic) {
    this.topic = topic;
  }
}
