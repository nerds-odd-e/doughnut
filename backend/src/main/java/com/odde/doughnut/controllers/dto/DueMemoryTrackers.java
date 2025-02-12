package com.odde.doughnut.controllers.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class DueMemoryTrackers extends RecallStatus {
  @Getter @Setter private List<MemoryTrackerLite> toRepeat;
  @Getter @Setter private Integer dueInDays;
}
