package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.RelationType;
import jakarta.validation.constraints.NotNull;

public class RelationshipCreation {
  @NotNull public RelationType relationType;
}
