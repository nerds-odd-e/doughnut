package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class DueMemoryTrackers {
  @NotNull public int toRepeatCount;
  @NotNull public int totalAssimilatedCount;
  @Getter @Setter private Timestamp recallWindowEndAt;
  @Getter @Setter private List<MemoryTrackerLite> toRepeat;
  @Getter @Setter private Integer dueInDays;
}
