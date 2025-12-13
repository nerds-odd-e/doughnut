package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.RecallSetting;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class NoteInfo {
  @Getter @Setter private List<MemoryTracker> memoryTrackers;
  @NotNull @Getter @Setter private NoteRealm note;
  @NotNull @Getter @Setter private Timestamp createdAt;
  @Getter @Setter public RecallSetting recallSetting;
  @Getter @Setter private NoteType noteType;
}
