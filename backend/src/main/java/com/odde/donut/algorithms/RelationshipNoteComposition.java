package com.odde.donut.algorithms;

import com.odde.donut.entities.Note;
import java.util.Optional;

/**
 * Title and Markdown of a relationship note (ADR 0004 shape), matching the frontend's {@code
 * relationshipNoteCompose.ts}, the relation naming rule between a property key and the {@code
 * relation} frontmatter value, and the reading of a relationship note's frontmatter.
 */
public final class RelationshipNoteComposition {
  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";

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

  /** The {@code relation}, {@code source} and {@code target} frontmatter of a relationship note. */
  public record Relationship(String relationScalar, String sourceScalar, String targetScalar) {
    /**
     * The property key this relationship reduces to: {@link #propertyKey(String)} with the
     * example-of family canonicalized. Null when the relation is missing or blank.
     */
    public String propertyKey() {
      String key = RelationshipNoteComposition.propertyKey(relationScalar);
      return key == null ? null : PropertyKeyNaming.canonicalExampleOfFamilyKey(key);
    }
  }

  /**
   * The relationship in {@code content}: present when its frontmatter has {@code type:
   * Relationship} (ignoring case) and non-blank {@code source} and {@code target}.
   */
  public static Optional<Relationship> parse(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .flatMap(lf -> {
          Frontmatter fm = lf.frontmatter();
          if (!RELATIONSHIP_NOTE_TYPE.equalsIgnoreCase(
              fm.getString("type").map(String::trim).orElse(""))) {
            return Optional.empty();
          }
          Optional<String> source =
              fm.getString("source").map(String::trim).filter(s -> !s.isEmpty());
          Optional<String> target =
              fm.getString("target").map(String::trim).filter(s -> !s.isEmpty());
          if (source.isEmpty() || target.isEmpty()) {
            return Optional.empty();
          }
          String relation = fm.getString("relation").map(String::trim).orElse(null);
          return Optional.of(new Relationship(relation, source.get(), target.get()));
        });
  }

  private static String yamlDoubleQuotedInner(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
