package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationType {
  RELATED_TO("related to"),
  SPECIALIZE("a specialization of"),
  APPLICATION("an application of"),
  INSTANCE("an instance of"),
  PART("a part of"),
  TAGGED_BY("tagged by"),
  ATTRIBUTE("an attribute of"),
  OPPOSITE_OF("the opposite of"),
  AUTHOR_OF("author of"),
  USES("using"),
  EXAMPLE_OF("an example of"),
  PRECEDES("before"),
  SIMILAR_TO("similar to"),
  CONFUSE_WITH("confused with");

  @JsonValue public final String label;

  RelationType(String label) {
    this.label = label;
  }

  public static RelationType fromLabel(String text) {
    for (RelationType b : RelationType.values()) {
      if (b.label.equalsIgnoreCase(text)) {
        return b;
      }
    }
    return null;
  }
}
