package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

/** Line-oriented edits and scalar formatting for YAML between leading {@code ---} fences. */
public final class NoteYamlFrontmatterEditing {

  private NoteYamlFrontmatterEditing() {}

  /** Drops existing {@code image} / {@code image_mask} lines and optionally appends new scalars. */
  public static List<String> yamlLinesReplacingImageScalars(
      String[] yamlLines, boolean hasImage, String imageUrl, String imageMask) {
    List<String> kept = new ArrayList<>();
    for (String line : yamlLines) {
      if (!isImageOrImageMaskPropertyLine(line)) {
        kept.add(line);
      }
    }
    if (hasImage && !Strings.isBlank(imageUrl)) {
      kept.add("image: " + formatYamlScalarValue(imageUrl.trim()));
      if (!Strings.isBlank(imageMask)) {
        kept.add("image_mask: " + formatYamlScalarValue(imageMask.trim()));
      }
    }
    return kept;
  }

  public static List<String> imageScalarLinesOnly(String imageUrl, String imageMask) {
    List<String> lines = new ArrayList<>();
    lines.add("image: " + formatYamlScalarValue(imageUrl.trim()));
    if (!Strings.isBlank(imageMask)) {
      lines.add("image_mask: " + formatYamlScalarValue(imageMask.trim()));
    }
    return lines;
  }

  public static boolean isImageOrImageMaskPropertyLine(String line) {
    String t = line.trim();
    if (t.isEmpty() || t.startsWith("#")) {
      return false;
    }
    int colon = t.indexOf(':');
    if (colon < 0) {
      return false;
    }
    String key = t.substring(0, colon).trim();
    return "image".equalsIgnoreCase(key) || "image_mask".equalsIgnoreCase(key);
  }

  public static boolean isEmptyPropertyLine(String line) {
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
}
