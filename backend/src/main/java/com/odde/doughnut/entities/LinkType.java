package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LinkType {
  NO_LINK(0, "no link", "no link", ""),
  RELATED_TO(1, "related note", "related to", "related to"),
  SPECIALIZE(2, "specification", "a specialization of", "a generalization of"),
  APPLICATION(3, "application", "an application of", "applied to"),
  INSTANCE(4, "instance", "an instance of", "has instances"),
  /*INTEGRATED*/ PART(6, "part", "a part of", "has parts"),
  /*NON INTEGRATED*/ TAGGED_BY(8, "tag target", "tagged by", "tagging"),
  ATTRIBUTE(10, "attribute", "an attribute of", "has attributes"),
  OPPOSITE_OF(12, "opposition", "the opposite of", "the opposite of"),
  AUTHOR_OF(14, "author", "author of", "brought by"),
  USES(15, "user", "using", "used by"),
  EXAMPLE_OF(17, "example", "an example of", "has examples"),
  PRECEDES(19, "precedence", "before", "after"),
  SIMILAR_TO(22, "note", "similar to", "similar to"),
  CONFUSE_WITH(23, "note", "confused with", "confused with");

  @JsonValue public final String label;
  public final String nameOfSource;
  public final Integer id;
  public final String reversedLabel;

  LinkType(Integer id, String nameOfSource, String label, String reversedLabel) {
    this.nameOfSource = nameOfSource;
    this.label = label;
    this.id = id;
    this.reversedLabel = reversedLabel;
  }

  public static LinkType fromLabel(String text) {
    for (LinkType b : LinkType.values()) {
      if (b.label.equalsIgnoreCase(text)) {
        return b;
      }
    }
    return null;
  }
}
