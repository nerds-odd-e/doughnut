package com.odde.doughnut.algorithms;

import java.util.List;
import java.util.Optional;

/** Leading {@code ---} fenced block at the start of note markdown: split and rebuild. */
public final class NoteLeadingFrontmatter {

  private NoteLeadingFrontmatter() {}

  public record Split(String yamlRaw, String body) {}

  public static Optional<Split> split(String content) {
    if (content == null || content.isEmpty()) {
      return Optional.empty();
    }
    String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
    String work = stripUtf8Bom(normalized);
    String[] lines = work.split("\n", -1);
    if (lines.length == 0 || !"---".equals(lines[0])) {
      return Optional.empty();
    }
    for (int i = 1; i < lines.length; i++) {
      if ("---".equals(lines[i])) {
        String yamlRaw = joinLines(lines, 1, i);
        String body = joinLines(lines, i + 1, lines.length);
        return Optional.of(new Split(yamlRaw, body));
      }
    }
    return Optional.empty();
  }

  /**
   * Note content whose first lines are a fenced YAML block. When {@code yamlLines} is empty,
   * returns {@code body} unchanged (no fences).
   */
  public static String contentWithLeadingFence(List<String> yamlLines, String body) {
    if (yamlLines.isEmpty()) {
      return body;
    }
    return "---\n" + String.join("\n", yamlLines) + "\n---\n" + body;
  }

  private static String stripUtf8Bom(String s) {
    if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
      return s.substring(1);
    }
    return s;
  }

  private static String joinLines(String[] lines, int fromIndex, int toIndex) {
    if (fromIndex >= toIndex) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int j = fromIndex; j < toIndex; j++) {
      if (j > fromIndex) {
        sb.append('\n');
      }
      sb.append(lines[j]);
    }
    return sb.toString();
  }
}
