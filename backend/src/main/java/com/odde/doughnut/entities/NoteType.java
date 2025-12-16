package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NoteType {
  CONCEPT("concept"),
  CATEGORY("category"),
  VOCAB("vocab"),
  JOURNAL("journal");

  @JsonValue public final String label;

  NoteType(String label) {
    this.label = label;
  }

  @JsonCreator
  public static NoteType fromLabel(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    for (NoteType type : NoteType.values()) {
      if (type.label.equalsIgnoreCase(text)) {
        return type;
      }
    }
    return CONCEPT; // Default if not found
  }
}
