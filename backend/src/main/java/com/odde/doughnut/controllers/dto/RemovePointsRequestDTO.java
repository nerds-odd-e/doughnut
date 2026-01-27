package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RemovePointsRequestDTO {
  private List<String> points;
}
