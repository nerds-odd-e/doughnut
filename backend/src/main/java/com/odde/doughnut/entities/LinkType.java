package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.*;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

public enum LinkType {
  NO_LINK(0, "no link", "no link", "", new QuizQuestionEntity.QuestionType[] {}),
  RELATED_TO(
      1, "related note", "related to", "related to", new QuizQuestionEntity.QuestionType[] {}),
  SPECIALIZE(
      2,
      "specification",
      "a specialization of",
      "a generalization of",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE,
        WHICH_SPEC_HAS_INSTANCE,
        FROM_SAME_PART_AS,
        FROM_DIFFERENT_PART_AS,
        DESCRIPTION_LINK_TARGET
      }),
  APPLICATION(
      3,
      "application",
      "an application of",
      "applied to",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE_WITHIN_SAME_LINK_TYPE,
        WHICH_SPEC_HAS_INSTANCE,
        FROM_SAME_PART_AS,
        FROM_DIFFERENT_PART_AS,
        DESCRIPTION_LINK_TARGET
      }),

  INSTANCE(
      4,
      "instance",
      "an instance of",
      "has instances",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE,
        WHICH_SPEC_HAS_INSTANCE,
        FROM_SAME_PART_AS,
        FROM_DIFFERENT_PART_AS,
        DESCRIPTION_LINK_TARGET
      }),
  /*INTEGRATED*/ PART(
      6,
      "part",
      "a part of",
      "has parts",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE,
        WHICH_SPEC_HAS_INSTANCE,
        FROM_SAME_PART_AS,
        FROM_DIFFERENT_PART_AS,
        DESCRIPTION_LINK_TARGET
      }),
  /*NON INTEGRATED*/ TAGGED_BY(
      8,
      "tag target",
      "tagged by",
      "tagging",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE,
        WHICH_SPEC_HAS_INSTANCE,
        WHICH_SPEC_HAS_INSTANCE,
        DESCRIPTION_LINK_TARGET
      }),
  ATTRIBUTE(
      10,
      "attribute",
      "an attribute of",
      "has attributes",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE,
        WHICH_SPEC_HAS_INSTANCE,
        WHICH_SPEC_HAS_INSTANCE,
        DESCRIPTION_LINK_TARGET
      }),

  OPPOSITE_OF(
      12,
      "opposition",
      "the opposite of",
      "the opposite of",
      new QuizQuestionEntity.QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  AUTHOR_OF(
      14,
      "author",
      "author of",
      "brought by",
      new QuizQuestionEntity.QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  USES(
      15,
      "user",
      "using",
      "used by",
      new QuizQuestionEntity.QuestionType[] {
        LINK_TARGET,
        LINK_SOURCE,
        WHICH_SPEC_HAS_INSTANCE,
        FROM_SAME_PART_AS,
        FROM_DIFFERENT_PART_AS,
        DESCRIPTION_LINK_TARGET
      }),
  EXAMPLE_OF(
      17,
      "example",
      "an example of",
      "has examples",
      new QuizQuestionEntity.QuestionType[] {
        LINK_SOURCE_WITHIN_SAME_LINK_TYPE,
        CLOZE_LINK_TARGET,
        FROM_SAME_PART_AS,
        FROM_DIFFERENT_PART_AS
      }),
  PRECEDES(
      19,
      "precedence",
      "before",
      "after",
      new QuizQuestionEntity.QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  SIMILAR_TO(
      22,
      "thing",
      "similar to",
      "similar to",
      new QuizQuestionEntity.QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
  CONFUSE_WITH(
      23, "thing", "confused with", "confused with", new QuizQuestionEntity.QuestionType[] {});

  @JsonValue public final String label;
  public final String nameOfSource;
  public final Integer id;
  public String reversedLabel;
  @Getter private final QuizQuestionEntity.QuestionType[] questionTypes;

  LinkType(
      Integer id,
      String nameOfSource,
      String label,
      String reversedLabel,
      QuizQuestionEntity.QuestionType[] questionTypes) {
    this.nameOfSource = nameOfSource;
    this.label = label;
    this.id = id;
    this.reversedLabel = reversedLabel;
    this.questionTypes = questionTypes;
  }

  private static final Map<Integer, LinkType> idMap =
      Collections.unmodifiableMap(
          Arrays.stream(values()).collect(Collectors.toMap(x -> x.id, x -> x)));

  public static LinkType fromLabel(String text) {
    for (LinkType b : LinkType.values()) {
      if (b.label.equalsIgnoreCase(text)) {
        return b;
      }
    }
    return null;
  }

  public static LinkType fromId(Integer id) {
    return idMap.getOrDefault(id, null);
  }
}
