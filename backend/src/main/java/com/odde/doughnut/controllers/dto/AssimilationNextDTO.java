package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.AssimilationUnit;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationNextDTO {
  private Integer nextNoteId;
  private String nextPropertyKey;
  private AssimilationCountDTO counts;

  public static AssimilationNextDTO from(
      Optional<AssimilationUnit> nextUnit, AssimilationCountDTO counts) {
    return new AssimilationNextDTO(
        nextUnit.map(unit -> unit.note().getId()).orElse(null),
        nextUnit
            .filter(AssimilationUnit::isPropertyLevel)
            .map(AssimilationUnit::propertyKey)
            .orElse(null),
        counts);
  }
}
