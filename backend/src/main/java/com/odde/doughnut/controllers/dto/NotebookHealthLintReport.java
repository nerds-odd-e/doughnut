package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotebookHealthLintReport {

  @Schema(description = "Finding groups, typically one per health rule")
  private List<HealthFindingGroup> groups;
}
