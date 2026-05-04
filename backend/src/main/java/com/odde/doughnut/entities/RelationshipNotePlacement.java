package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationshipNotePlacement {
  RELATIONS_SUBFOLDER("relations_subfolder"),
  SAME_LEVEL_AS_SOURCE("same_level_as_source"),
  NAMED_AFTER_SOURCE_NOTE("named_after_source_note");

  @JsonValue public final String apiValue;

  RelationshipNotePlacement(String apiValue) {
    this.apiValue = apiValue;
  }
}
