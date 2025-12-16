package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelationType {
  NO_LINK("no link", null),
  RELATED_TO(
      "related to",
      """
      **Special Instruction for Relation Note (related to)**: The focus note represents a relationship
      where the subject is related to the object. When generating the question, emphasize the
      connection and relationship between these two concepts. Consider asking about how they relate,
      what connects them, or the nature of their relationship.
      """),
  SPECIALIZE(
      "a specialization of",
      """
      **Special Instruction for Relation Note (a specialization of)**: The focus note represents a
      specialization relationship where the subject is a specific type or instance of the object.
      When generating the question, focus on the specific characteristics, properties, or details
      that make the subject a specialization of the object. Consider asking about distinguishing
      features, specific attributes, or what makes this specialization unique.
      """),
  APPLICATION(
      "an application of",
      """
      **Special Instruction for Relation Note (an application of)**: The focus note represents an
      application relationship where the subject applies or uses the object concept. When generating
      the question, emphasize how the subject applies the object concept in practice. Consider
      asking about practical uses, real-world applications, or how the concept is implemented.
      """),
  INSTANCE(
      "an instance of",
      """
      **Special Instruction for Relation Note (an instance of)**: The focus note represents an
      instance relationship where the subject is a concrete example of the object. When generating
      the question, focus on the specific characteristics of this instance. Consider asking about
      what makes this instance unique, its specific properties, or how it exemplifies the object
      concept.
      """),
  PART(
      "a part of",
      """
      **Special Instruction for Relation Note (a part of)**: The focus note represents a part-whole
      relationship where the subject is a component of the object. When generating the question,
      emphasize the role of the subject as a part of the whole. Consider asking about the function
      of this part, how it contributes to the whole, or its relationship to other parts.
      """),
  TAGGED_BY(
      "tagged by",
      """
      **Special Instruction for Relation Note (tagged by)**: The focus note represents a tagging
      relationship where the subject is tagged by the object. When generating the question, focus
      on the categorization or classification aspect. Consider asking about why this tag is
      relevant, what characteristics make this tag appropriate, or how this tag helps organize the
      knowledge.
      """),
  ATTRIBUTE(
      "an attribute of",
      """
      **Special Instruction for Relation Note (an attribute of)**: The focus note represents an
      attribute relationship where the subject is a property or characteristic of the object. When
      generating the question, emphasize the attribute and its significance. Consider asking about
      the value of this attribute, how it characterizes the object, or its importance in
      understanding the object.
      """),
  OPPOSITE_OF(
      "the opposite of",
      """
      **Special Instruction for Relation Note (the opposite of)**: The focus note represents an
      opposition relationship where the subject is the opposite of the object. When generating
      the question, emphasize the contrast and opposition between these concepts. Consider asking
      about the differences, what makes them opposite, or how they contrast with each other.
      """),
  AUTHOR_OF(
      "author of",
      """
      **Special Instruction for Relation Note (author of)**: The focus note represents an
      authorship relationship where the subject is the author or creator of the object. When
      generating the question, focus on the creative or authorship aspect. Consider asking about
      the author's contribution, their role in creating the object, or characteristics of their
      work.
      """),
  USES(
      "using",
      """
      **Special Instruction for Relation Note (using)**: The focus note represents a usage
      relationship where the subject uses the object. When generating the question, emphasize how
      the subject utilizes or employs the object. Consider asking about the purpose of usage, how
      it's used, or the benefits of using this object.
      """),
  EXAMPLE_OF(
      "an example of",
      """
      **Special Instruction for Relation Note (an example of)**: The focus note represents an example
      relationship where the subject exemplifies the object. When generating the question, focus on
      how the subject serves as an example. Consider asking about what makes it a good example, its
      representative characteristics, or how it illustrates the object concept.
      """),
  PRECEDES(
      "before",
      """
      **Special Instruction for Relation Note (before)**: The focus note represents a temporal or
      sequential relationship where the subject comes before the object. When generating the
      question, emphasize the sequence, order, or temporal relationship. Consider asking about what
      comes before, the sequence of events, or the chronological relationship.
      """),
  SIMILAR_TO(
      "similar to",
      """
      **Special Instruction for Relation Note (similar to)**: The focus note represents a similarity
      relationship where the subject is similar to the object. When generating the question,
      emphasize the similarities and commonalities. Consider asking about shared characteristics,
      what makes them similar, or how they compare.
      """),
  CONFUSE_WITH(
      "confused with",
      """
      **Special Instruction for Relation Note (confused with)**: The focus note represents a confusion
      relationship where the subject is often confused with the object. When generating the
      question, emphasize the distinction and differences to help clarify the confusion. Consider
      asking about how to distinguish them, what makes them different, or common misconceptions.
      """);

  @JsonValue public final String label;
  public final String questionGenerationInstruction;

  RelationType(String label, String questionGenerationInstruction) {
    this.label = label;
    this.questionGenerationInstruction = questionGenerationInstruction;
  }

  public String getQuestionGenerationInstruction() {
    return questionGenerationInstruction;
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
