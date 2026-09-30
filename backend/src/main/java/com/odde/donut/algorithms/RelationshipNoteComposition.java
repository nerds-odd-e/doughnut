package com.odde.donut.algorithms;

import com.odde.donut.entities.Note;

/**
 * Title and Markdown of a relationship note (ADR 0004 shape), matching the frontend's {@code
 * relationshipNoteCompose.ts}, and the relation naming rule between a property key and the {@code
 * relation} frontmatter value.
 */
public final class RelationshipNoteComposition {
  private RelationshipNoteComposition() {}

  public static String title(String sourceTitle, String relationLabel, String targetTitle) {
    String composed = sourceTitle + " " + relationLabel + " " + targetTitle;
    return composed.length() > Note.MAX_TITLE_LENGTH
        ? composed.substring(0, Note.MAX_TITLE_LENGTH)
        : composed;
  }

  public static String markdown(String relationLabel, String sourceLink, String targetLink) {
    return "---\n"
        + "type: Relationship\n"
        + "relation: "
        + relationLabel.trim().toLowerCase().replaceAll("\\s+", "-")
        + "\n"
        + "source: \""
        + yamlDoubleQuotedInner(sourceLink)
        + "\"\n"
        + "target: \""
        + yamlDoubleQuotedInner(targetLink)
        + "\"\n"
        + "---\n\n";
  }

  /**
   * Property key for a {@code relation} frontmatter value; same rule as frontend {@code
   * relationTypeFromKebab}: hyphens become spaces, trimmed. Null when nothing remains.
   */
  public static String propertyKey(String relation) {
    if (relation == null) {
      return null;
    }
    String derived = relation.replace('-', ' ').trim();
    return derived.isEmpty() ? null : derived;
  }

  private static String yamlDoubleQuotedInner(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
