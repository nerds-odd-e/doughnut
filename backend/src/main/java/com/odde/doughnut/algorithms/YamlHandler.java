package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Line-oriented helpers for simple {@code key: value} YAML fragments (no document model). */
public final class YamlHandler {

  private YamlHandler() {}

  /**
   * The trimmed value for the first line whose key equals {@code key} (case-insensitive), with
   * optional surrounding single/double quotes removed from the value.
   */
  public static Optional<String> firstScalarValue(String yamlText, String key) {
    if (yamlText == null || yamlText.isEmpty()) {
      return Optional.empty();
    }
    String normalized = yamlText.replace("\r\n", "\n").replace('\r', '\n');
    for (String line : normalized.split("\n", -1)) {
      String t = line.trim();
      if (t.isEmpty() || t.startsWith("#")) {
        continue;
      }
      int colon = t.indexOf(':');
      if (colon < 0) {
        continue;
      }
      String lineKey = t.substring(0, colon).trim();
      if (!lineKey.equalsIgnoreCase(key)) {
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

  /**
   * Drops lines that start a mapping entry whose key is in {@code keysToOmit} (case-insensitive).
   */
  public static List<String> linesOmittingMappingKeys(String[] yamlLines, Set<String> keysToOmit) {
    List<String> kept = new ArrayList<>();
    for (String line : yamlLines) {
      if (!lineOpensMappingWithKeyIn(line, keysToOmit)) {
        kept.add(line);
      }
    }
    return kept;
  }

  /** One {@code key: formatted-value} line for simple scalar serialization. */
  public static String mappingLine(String key, String rawValue) {
    return key + ": " + formatYamlScalarValue(rawValue);
  }

  public static boolean isEmptyMappingValueLine(String line) {
    String t = line.trim();
    int colon = t.indexOf(':');
    if (colon < 0) {
      return false;
    }
    String value = t.substring(colon + 1).trim();
    return value.isEmpty() || value.equals("\"\"") || value.equals("''");
  }

  /** Plain scalar when safe; else double-quoted with minimal escaping. */
  public static String formatYamlScalarValue(String value) {
    if (value.isEmpty()) {
      return "\"\"";
    }
    boolean safePlain =
        value
                .chars()
                .noneMatch(
                    c ->
                        c == '\n' || c == '\r' || c == '"' || c == '\'' || c == '#' || c == ':'
                            || c == ' ' || c == '\t')
            && !value.startsWith("-")
            && !value.startsWith("@")
            && !value.startsWith("*")
            && !value.startsWith("&")
            && !value.startsWith("!")
            && !value.startsWith("%")
            && !value.startsWith("|")
            && !value.startsWith(">")
            && !value.equals("true")
            && !value.equals("false")
            && !value.equalsIgnoreCase("null");
    if (safePlain) {
      return value;
    }
    return "\""
        + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        + "\"";
  }

  private static boolean lineOpensMappingWithKeyIn(String line, Set<String> keys) {
    return mappingKeyFromLine(line)
        .filter(k -> keys.stream().anyMatch(k::equalsIgnoreCase))
        .isPresent();
  }

  private static Optional<String> mappingKeyFromLine(String line) {
    String t = line.trim();
    if (t.isEmpty() || t.startsWith("#")) {
      return Optional.empty();
    }
    int colon = t.indexOf(':');
    if (colon < 0) {
      return Optional.empty();
    }
    String key = t.substring(0, colon).trim();
    return Optional.of(key);
  }
}
