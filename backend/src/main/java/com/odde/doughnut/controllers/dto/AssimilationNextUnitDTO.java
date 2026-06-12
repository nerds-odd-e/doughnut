package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.AssimilationUnit;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationNextUnitDTO {
  private int noteId;
  private String propertyKey;

  public static AssimilationNextUnitDTO from(AssimilationUnit unit) {
    return new AssimilationNextUnitDTO(
        unit.note().getId(), unit.isPropertyLevel() ? unit.propertyKey() : null);
  }
}
