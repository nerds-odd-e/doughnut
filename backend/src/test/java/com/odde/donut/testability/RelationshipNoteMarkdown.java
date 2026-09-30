package com.odde.donut.testability;

import com.odde.donut.algorithms.RelationshipNoteComposition;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;

/** Builds relationship-note frontmatter markdown for test fixtures. */
public final class RelationshipNoteMarkdown {
  private static final String UNTITLED = "Untitled";
  private static final String DEFAULT_RELATION_LABEL = "related to";

  private RelationshipNoteMarkdown() {}

  public static String forEndpoints(
      Note relationshipNote,
      String relationLabelOrNull,
      Note sourceEndpoint,
      Note targetEndpoint,
      String preservedDetailsOrNull) {
    StringBuilder out =
        new StringBuilder(
            RelationshipNoteComposition.markdown(
                resolveRelationLabel(relationLabelOrNull),
                wikiTokenForEndpoint(relationshipNote, sourceEndpoint),
                wikiTokenForEndpoint(relationshipNote, targetEndpoint)));
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
}
