package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.NoteRecallSetting;
import com.odde.doughnut.entities.NoteType;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class NoteRecallInfo {
  @Getter @Setter private List<MemoryTracker> memoryTrackers;
  @Getter @Setter public NoteRecallSetting recallSetting;
  @Getter @Setter private NoteType noteType;
}
