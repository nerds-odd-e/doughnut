package com.odde.donut.algorithms;

/** Persist-time note concept type for stored markdown. */
public final class NoteConceptType {

  private static final String ORDINARY_TYPE = "Note";
  private static final String RELATIONSHIP_TYPE = "Relationship";

  private NoteConceptType() {}

  public static String ensureStoredType(String content) {
    return NoteLeadingFrontmatter.ensureTypeKey(
        content, ORDINARY_TYPE, ORDINARY_TYPE, RELATIONSHIP_TYPE);
  }

  public static boolean isOrdinary(String content) {
    return NoteLeadingFrontmatter.split(content)
        .flatMap(s -> s.frontmatter().getString("type"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(type -> ORDINARY_TYPE.equalsIgnoreCase(type))
        .orElse(true);
  }
}
