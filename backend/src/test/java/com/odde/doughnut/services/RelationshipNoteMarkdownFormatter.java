package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;

/**
 * Test fixture: builds relationship-note markdown for DB and fixture setup (rules match the
 * product’s relationship notes).
 */
public final class RelationshipNoteMarkdownFormatter {

  private static final String NOTE_TYPE = "relationship";
  private static final String UNTITLED = "Untitled";
  private static final String DEFAULT_RELATION_LABEL = "related to";

  private RelationshipNoteMarkdownFormatter() {}

  public static String relationKebabFromLabel(String label) {
    String t = trimmedOrEmpty(label);
    if (t.isEmpty()) {
      return relationKebabFromLabel(DEFAULT_RELATION_LABEL);
    }
    return t.toLowerCase().replaceAll("\\s+", "-");
  }

  public static String format(
      String relationLabelOrNull,
      String sourceTitle,
      String targetTitle,
      String preservedDetailsOrNull) {
    String relationLabel = resolveRelationLabel(relationLabelOrNull);
    return buildDocument(
        relationLabel,
        wikiLink(displayTitle(sourceTitle)),
        wikiLink(displayTitle(targetTitle)),
        preservedDetailsOrNull);
  }

  public static String formatForRelationshipNote(
      Note relationshipNote,
      String relationLabelOrNull,
      Note sourceEndpoint,
      Note targetEndpoint,
      String preservedDetailsOrNull) {
    String relationLabel = resolveRelationLabel(relationLabelOrNull);
    return buildDocument(
        relationLabel,
        wikiTokenForEndpoint(relationshipNote, sourceEndpoint),
        wikiTokenForEndpoint(relationshipNote, targetEndpoint),
        preservedDetailsOrNull);
  }

  private static String resolveRelationLabel(String relationLabelOrNull) {
    if (relationLabelOrNull == null || trimmedOrEmpty(relationLabelOrNull).isEmpty()) {
      return DEFAULT_RELATION_LABEL;
    }
    return relationLabelOrNull.trim();
  }

  private static String buildDocument(
      String relationLabel, String sourceLink, String targetLink, String preservedDetailsOrNull) {
    String relationKebab = relationKebabFromLabel(relationLabel);
    String bodyLine = sourceLink + " " + relationLabel + " " + targetLink + ".";

    StringBuilder out = new StringBuilder();
    out.append("---\n");
    out.append("type: ").append(NOTE_TYPE).append('\n');
    out.append("relation: ").append(relationKebab).append('\n');
    out.append("source: \"").append(yamlDoubleQuotedInner(sourceLink)).append("\"\n");
    out.append("target: \"").append(yamlDoubleQuotedInner(targetLink)).append("\"\n");
    out.append("---\n\n");
    out.append(bodyLine);

    String preserved = trimmedOrNull(preservedDetailsOrNull);
    if (preserved != null) {
      out.append("\n\n").append(preserved);
    }
    return out.toString();
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
