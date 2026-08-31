package com.odde.donut.controllers.dto;

import com.odde.donut.entities.MemoryTracker;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class NoteRecallInfo {
  @Getter @Setter private List<MemoryTracker> memoryTrackers;
  @Getter @Setter private List<String> skippedPropertyKeys;
}
