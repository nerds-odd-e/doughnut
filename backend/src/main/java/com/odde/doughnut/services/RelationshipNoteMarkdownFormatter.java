package com.odde.doughnut.services;

import com.odde.doughnut.entities.RelationType;

public final class RelationshipNoteMarkdownFormatter {

  private static final String NOTE_TYPE = "relationship";
  private static final String UNTITLED = "Untitled";

  private RelationshipNoteMarkdownFormatter() {}

  public static String extractUserSuffixFromRelationshipDetails(String details) {
    if (details == null) {
      return null;
    }
    String n = details.replace("\r\n", "\n");
    if (!n.startsWith("---\n")) {
      return null;
    }
    int sep = n.indexOf("\n---\n\n", 1);
    if (sep < 0) {
      return null;
    }
    String frontmatterBlock = n.substring(4, sep);
    if (!frontmatterBlock.contains("type: relationship")) {
      return null;
    }
    String afterFm = n.substring(sep + 6);
    int firstNl = afterFm.indexOf('\n');
    if (firstNl < 0) {
      return null;
    }
    String rest = afterFm.substring(firstNl + 1);
    if (rest.isEmpty()) {
      return null;
    }
    if (rest.startsWith("\n\n")) {
      rest = rest.substring(2);
    } else if (rest.startsWith("\n")) {
      rest = rest.substring(1);
    }
    return trimmedOrNull(rest);
  }

  public static String format(
      RelationType relationType,
      String sourceTitle,
      String targetTitle,
      String preservedDetailsOrNull) {
    RelationType type = relationType != null ? relationType : RelationType.RELATED_TO;
    String relationLabel = type.label;
    String relationKebab = labelToKebab(relationLabel);

    String sourceDisplay = displayTitle(sourceTitle);
    String targetDisplay = displayTitle(targetTitle);
    String sourceLink = wikiLink(sourceDisplay);
    String targetLink = wikiLink(targetDisplay);

    String bodyLine = bodyLine(sourceLink, relationLabel, targetLink);

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

  private static String bodyLine(String sourceLink, String relationLabel, String targetLink) {
    return sourceLink + " " + relationLabel + " " + targetLink + ".";
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

  private static String labelToKebab(String label) {
    String t = trimmedOrEmpty(label);
    if (t.isEmpty()) {
      return labelToKebab(RelationType.RELATED_TO.label);
    }
    return t.toLowerCase().replaceAll("\\s+", "-");
  }

  private static String yamlDoubleQuotedInner(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
