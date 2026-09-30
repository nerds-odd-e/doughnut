package com.odde.donut.controllers.dto;

import com.odde.donut.services.AssimilationUnit;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationNextUnitDTO {
  private int noteId;
  private String propertyKey;
  private String propertyValue;

  public static AssimilationNextUnitDTO from(AssimilationUnit unit) {
    if (!unit.isPropertyLevel()) {
      return new AssimilationNextUnitDTO(unit.note().getId(), null, null);
    }
    return new AssimilationNextUnitDTO(
        unit.note().getId(), unit.propertyKey(), unit.propertyValue());
  }
}
