package com.odde.donut.controllers.dto;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.PropertyFocus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemoryTrackerLite {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int memoryTrackerId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean spelling;

  private String propertyKey;

  private String propertyValue;

  public static MemoryTrackerLite from(MemoryTracker memoryTracker) {
    MemoryTrackerLite lite = new MemoryTrackerLite();
    lite.setMemoryTrackerId(memoryTracker.getId());
    lite.setSpelling(memoryTracker.isSpelling());
    PropertyFocus propertyFocus = memoryTracker.propertyFocus();
    if (propertyFocus != null) {
      lite.setPropertyKey(propertyFocus.key());
      lite.setPropertyValue(propertyFocus.listItemOrNull());
    }
    return lite;
  }
}
