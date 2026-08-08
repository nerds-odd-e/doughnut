package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class DueMemoryTrackers {
  @NotNull public int totalAssimilatedCount;
  @Getter @Setter private Timestamp currentRecallWindowEndAt;
  @Getter @Setter private List<MemoryTrackerLite> toRepeat;
  @Getter @Setter private List<DueCommissionedMemoryTrackerLite> dueCommissioned;
  @Getter @Setter private List<AwaitingReportLearningSessionLite> awaitingReportSessions;
  @Getter @Setter private List<RecordedLearningSessionLite> recordedSessions;
  @Getter @Setter private Integer dueInDays;
}
