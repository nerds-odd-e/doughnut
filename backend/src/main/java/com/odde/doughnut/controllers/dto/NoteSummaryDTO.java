package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NoteSummaryDTO {
  private List<String> points;

  public NoteSummaryDTO(List<String> points) {
    this.points = points;
  }
}
