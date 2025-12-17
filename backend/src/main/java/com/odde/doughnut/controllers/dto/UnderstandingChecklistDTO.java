package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UnderstandingChecklistDTO {
  private List<String> points;

  public UnderstandingChecklistDTO(List<String> points) {
    this.points = points;
  }
}
