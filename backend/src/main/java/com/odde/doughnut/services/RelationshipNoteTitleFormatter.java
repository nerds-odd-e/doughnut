package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;

public final class RelationshipNoteTitleFormatter {

  private static final String UNTITLED = "Untitled";

  private RelationshipNoteTitleFormatter() {}

  public static String format(String sourceTitle, String relationLabel, String targetTitle) {
    String source = displaySegment(trimmedOrEmpty(sourceTitle), UNTITLED);
    String relation = displaySegment(trimmedOrEmpty(relationLabel), RelationType.RELATED_TO.label);
    String target = displaySegment(trimmedOrEmpty(targetTitle), UNTITLED);
    String composed = (source + " " + relation + " " + target).trim();
    if (composed.isEmpty()) {
      return UNTITLED;
    }
    if (composed.length() > Note.MAX_TITLE_LENGTH) {
      return composed.substring(0, Note.MAX_TITLE_LENGTH);
    }
    return composed;
  }

  private static String trimmedOrEmpty(String s) {
    if (s == null) {
      return "";
    }
    return s.trim();
  }

  private static String displaySegment(String trimmed, String fallbackWhenBlank) {
    return trimmed.isEmpty() ? fallbackWhenBlank : trimmed;
  }
}
