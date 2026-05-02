package com.odde.doughnut.algorithms;

import java.util.Optional;

/** First {@code key: value} scalar in a YAML frontmatter block (simple line-based notes format). */
public final class NoteYamlFrontmatterScalars {

  private NoteYamlFrontmatterScalars() {}

  /**
   * The trimmed value for the first line whose key equals {@code fieldKey} (case-insensitive), with
   * optional surrounding single/double quotes removed from the value.
   */
  public static Optional<String> firstScalarValue(String yamlRaw, String fieldKey) {
    if (yamlRaw == null || yamlRaw.isEmpty()) {
      return Optional.empty();
    }
    String normalized = yamlRaw.replace("\r\n", "\n").replace('\r', '\n');
    for (String line : normalized.split("\n", -1)) {
      String t = line.trim();
      if (t.isEmpty() || t.startsWith("#")) {
        continue;
      }
      int colon = t.indexOf(':');
      if (colon < 0) {
        continue;
      }
      String key = t.substring(0, colon).trim();
      if (!key.equalsIgnoreCase(fieldKey)) {
        continue;
      }
      String value = t.substring(colon + 1).trim();
      if (value.isEmpty()) {
        return Optional.empty();
      }
      if (value.length() >= 2) {
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
          value = value.substring(1, value.length() - 1).trim();
        }
      }
      return Optional.of(value);
    }
    return Optional.empty();
  }
}
