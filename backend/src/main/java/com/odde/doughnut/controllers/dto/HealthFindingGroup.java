package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthFindingGroup {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Stable rule id")
  private String ruleId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Human-readable rule title")
  private String title;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rule severity")
  private HealthSeverity severity;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Whether findings in this group can be auto-fixed")
  private boolean autoFixable;

  @Schema(description = "Leaf findings for this group")
  private List<HealthFindingItem> items;

  @Schema(description = "Nested finding groups (e.g. per-note dead links)")
  private List<HealthFindingGroup> children;
}
