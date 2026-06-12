package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.AssimilationUnit;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssimilationNextDTO {
  private AssimilationNextUnitDTO nextUnit;
  private AssimilationCountDTO counts;

  public static AssimilationNextDTO from(
      Optional<AssimilationUnit> nextUnit, AssimilationCountDTO counts) {
    return new AssimilationNextDTO(
        nextUnit.map(AssimilationNextUnitDTO::from).orElse(null), counts);
  }
}
