package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RemovePointsRequestDTO {
  @Schema(description = "Points to remove from the note details. AI will regenerate details without these.")
  public List<String> points;
}
