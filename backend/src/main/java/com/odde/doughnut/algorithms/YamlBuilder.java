package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Line-oriented helpers for simple {@code key: value} YAML fragments (no document model). */
public final class YamlBuilder {

  private final String yamlText;

  public YamlBuilder(String yamlText) {
    this.yamlText = yamlText == null ? "" : yamlText.replace("\r\n", "\n").replace('\r', '\n');
  }

  /**
   * Returns a new builder with one more {@code key: formatted-value} line (does not read stored
   * content).
   */
  public YamlBuilder appendMapping(String key, String rawValue) {
    String line = mappingLine(key, rawValue);
    if (yamlText.isEmpty()) {
      return new YamlBuilder(line);
    }
    return new YamlBuilder(yamlText + "\n" + line);
  }

  /**
   * Lines of the stored YAML, split like {@link String#split(String, int)} with limit {@code -1}.
   */
  public List<String> lines() {
    return new ArrayList<>(Arrays.asList(yamlText.split("\n", -1)));
  }

  /**
   * The trimmed value for the first line whose key equals {@code key} (case-insensitive), with
   * optional surrounding single/double quotes removed from the value.
   */
  public Optional<String> firstScalarValue(String key) {
    if (yamlText.isEmpty()) {
      return Optional.empty();
    }
    for (String line : yamlText.split("\n", -1)) {
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
  public List<String> linesOmittingMappingKeys(Set<String> keysToOmit) {
    List<String> kept = new ArrayList<>();
    for (String line : yamlText.split("\n", -1)) {
      if (!lineOpensMappingWithKeyIn(line, keysToOmit)) {
        kept.add(line);
      }
    }
    return kept;
  }

  /** One {@code key: formatted-value} line for simple scalar serialization. */
  public String mappingLine(String key, String rawValue) {
    return key + ": " + formatYamlScalarValue(rawValue);
  }

  public boolean isEmptyMappingValueLine(String line) {
    String t = line.trim();
    int colon = t.indexOf(':');
    if (colon < 0) {
      return false;
    }
    String value = t.substring(colon + 1).trim();
    return value.isEmpty() || value.equals("\"\"") || value.equals("''");
  }

  /** Plain scalar when safe; else double-quoted with minimal escaping. */
  public String formatYamlScalarValue(String value) {
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

  private boolean lineOpensMappingWithKeyIn(String line, Set<String> keys) {
    return mappingKeyFromLine(line)
        .filter(k -> keys.stream().anyMatch(k::equalsIgnoreCase))
        .isPresent();
  }

  private Optional<String> mappingKeyFromLine(String line) {
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
