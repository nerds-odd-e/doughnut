package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.RelationshipNotePlacement;
import jakarta.validation.constraints.NotNull;

public class RelationshipCreation {
  @NotNull public RelationType relationType;

  /** When null, the API uses {@link RelationshipNotePlacement#RELATIONS_SUBFOLDER}. */
  public RelationshipNotePlacement relationshipNotePlacement;
}
