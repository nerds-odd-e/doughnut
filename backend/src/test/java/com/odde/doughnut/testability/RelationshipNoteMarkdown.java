package com.odde.doughnut.testability;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;

/** Builds relationship-note frontmatter markdown for test fixtures. */
public final class RelationshipNoteMarkdown {
  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";
  private static final String UNTITLED = "Untitled";
  private static final String DEFAULT_RELATION_LABEL = "related to";

  private RelationshipNoteMarkdown() {}

  public static String forEndpoints(
      Note relationshipNote,
      String relationLabelOrNull,
      Note sourceEndpoint,
      Note targetEndpoint,
      String preservedDetailsOrNull) {
    String relationLabel = resolveRelationLabel(relationLabelOrNull);
    String relationKebab = relationKebabFromLabel(relationLabel);
    String sourceLink = wikiTokenForEndpoint(relationshipNote, sourceEndpoint);
    String targetLink = wikiTokenForEndpoint(relationshipNote, targetEndpoint);
    StringBuilder out = new StringBuilder();
    out.append("---\n");
    out.append("type: ").append(RELATIONSHIP_NOTE_TYPE).append('\n');
    out.append("relation: ").append(relationKebab).append('\n');
    out.append("source: \"").append(yamlDoubleQuotedInner(sourceLink)).append("\"\n");
    out.append("target: \"").append(yamlDoubleQuotedInner(targetLink)).append("\"\n");
    out.append("---\n\n");
    String preserved = trimmedOrNull(preservedDetailsOrNull);
    if (preserved != null) {
      out.append("\n\n").append(preserved);
    }
    return out.toString();
  }

  private static String resolveRelationLabel(String relationLabelOrNull) {
    if (relationLabelOrNull == null || trimmedOrEmpty(relationLabelOrNull).isEmpty()) {
      return DEFAULT_RELATION_LABEL;
    }
    return relationLabelOrNull.trim();
  }

  private static String relationKebabFromLabel(String label) {
    String t = trimmedOrEmpty(label);
    if (t.isEmpty()) {
      return relationKebabFromLabel(DEFAULT_RELATION_LABEL);
    }
    return t.toLowerCase().replaceAll("\\s+", "-");
  }

  private static String wikiTokenForEndpoint(Note relationshipNote, Note endpoint) {
    if (endpoint == null) {
      return wikiLink(UNTITLED);
    }
    String display = displayTitle(endpoint.getTitle());
    if (relationshipNote == null || sameNotebookAs(relationshipNote, endpoint)) {
      return wikiLink(display);
    }
    Notebook endNb = endpoint.getNotebook();
    if (endNb == null) {
      return wikiLink(display);
    }
    String nbName = trimmedOrEmpty(endNb.getName());
    if (nbName.isEmpty()) {
      return wikiLink(display);
    }
    return wikiLink(nbName + ": " + display);
  }

  private static boolean sameNotebookAs(Note relationshipNote, Note endpoint) {
    Notebook relNb = relationshipNote.getNotebook();
    Notebook endNb = endpoint.getNotebook();
    if (relNb == null || endNb == null) {
      return true;
    }
    Integer rid = relNb.getId();
    Integer eid = endNb.getId();
    if (rid != null && eid != null) {
      return rid.equals(eid);
    }
    return relNb == endNb;
  }

  private static String wikiLink(String displayTitle) {
    return "[[" + displayTitle + "]]";
  }

  private static String displayTitle(String title) {
    String t = trimmedOrEmpty(title);
    return t.isEmpty() ? UNTITLED : t;
  }

  private static String trimmedOrEmpty(String s) {
    if (s == null) {
      return "";
    }
    return s.trim();
  }

  private static String trimmedOrNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String yamlDoubleQuotedInner(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
