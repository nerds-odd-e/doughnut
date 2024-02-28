package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class DueReviewPoints {
  @Getter @Setter private List<Integer> toRepeat;
  @Getter @Setter private Integer dueInDays;
}
