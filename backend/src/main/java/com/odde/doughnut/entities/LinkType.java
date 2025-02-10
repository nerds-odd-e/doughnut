package com.odde.doughnut.entities;

import static com.odde.doughnut.services.LinkQuestionType.*;

import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.services.LinkQuestionType;
import lombok.Getter;

public enum LinkType {
  NO_LINK(0, "no link", "no link", "", new LinkQuestionType[] {}),
  RELATED_TO(1, "related note", "related to", "related to", new LinkQuestionType[] {}),
  SPECIALIZE(
      2,
      "specification",
      "a specialization of",
      "a generalization of",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  APPLICATION(
      3,
      "application",
      "an application of",
      "applied to",
      new LinkQuestionType[] {LINK_SOURCE_WITHIN_SAME_LINK_TYPE, DESCRIPTION_LINK_TARGET}),

  INSTANCE(
      4,
      "instance",
      "an instance of",
      "has instances",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  /*INTEGRATED*/ PART(
      6,
      "part",
      "a part of",
      "has parts",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  /*NON INTEGRATED*/ TAGGED_BY(
      8,
      "tag target",
      "tagged by",
      "tagging",
      new LinkQuestionType[] {LINK_SOURCE, WHICH_SPEC_HAS_INSTANCE, DESCRIPTION_LINK_TARGET}),
  ATTRIBUTE(
      10,
      "attribute",
      "an attribute of",
      "has attributes",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),

  OPPOSITE_OF(
      12,
      "opposition",
      "the opposite of",
      "the opposite of",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  AUTHOR_OF(
      14,
      "author",
      "author of",
      "brought by",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  USES(
      15,
      "user",
      "using",
      "used by",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  EXAMPLE_OF(
      17,
      "example",
      "an example of",
      "has examples",
      new LinkQuestionType[] {
        LINK_SOURCE_WITHIN_SAME_LINK_TYPE, CLOZE_LINK_TARGET,
      }),
  PRECEDES(
      19,
      "precedence",
      "before",
      "after",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  SIMILAR_TO(
      22,
      "note",
      "similar to",
      "similar to",
      new LinkQuestionType[] {LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  CONFUSE_WITH(23, "note", "confused with", "confused with", new LinkQuestionType[] {});

  @JsonValue public final String label;
  public final String nameOfSource;
  public final Integer id;
  public String reversedLabel;
  @Getter private final LinkQuestionType[] questionTypes;

  LinkType(
      Integer id,
      String nameOfSource,
      String label,
      String reversedLabel,
      LinkQuestionType[] questionTypes) {
    this.nameOfSource = nameOfSource;
    this.label = label;
    this.id = id;
    this.reversedLabel = reversedLabel;
    this.questionTypes = questionTypes;
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
