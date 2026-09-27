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
}
