package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;

/** Leading YAML frontmatter fence handling for note content markdown. */
public final class NoteContentMarkdown {

  private NoteContentMarkdown() {}

  public record LeadingFrontmatter(String yamlRaw, String body) {}

  public static String bodyWithoutLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content).map(LeadingFrontmatter::body).orElse(content);
  }

  /**
   * Reads a Wikidata Q-id from the first YAML frontmatter block when present, using {@code
   * wikidata_id} (rich-mode property rows).
   */
  public static Optional<String> wikidataIdScalarFromLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content)
        .flatMap(lf -> NoteYamlFrontmatterScalars.firstScalarValue(lf.yamlRaw(), "wikidata_id"));
  }

  public static Optional<LeadingFrontmatter> splitLeadingFrontmatter(String content) {
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
        return Optional.of(new LeadingFrontmatter(yamlRaw, body));
      }
    }
    return Optional.empty();
  }

  /**
   * Updates or removes {@code image:} and {@code image_mask:} scalar lines in the first leading
   * YAML frontmatter block so note display can read a single source of truth from {@code
   * note.content}. When {@code hasImage} is false, both lines are removed if present.
   */
  public static String mergeNoteImageScalarsIntoContent(
      String content, boolean hasImage, String imageUrl, String imageMask) {
    if (content == null) {
      content = "";
    }
    Optional<LeadingFrontmatter> split = splitLeadingFrontmatter(content);
    if (split.isEmpty()) {
      if (!hasImage) {
        return content;
      }
      String yamlAddition = yamlLinesForImageScalars(imageUrl, imageMask);
      return "---\n" + yamlAddition + "---\n" + content;
    }
    LeadingFrontmatter lf = split.get();
    List<String> kept =
        yamlLinesWithoutImageKeys(lf.yamlRaw().split("\n", -1), hasImage, imageUrl, imageMask);
    String body = lf.body();
    if (kept.isEmpty()) {
      return body;
    }
    return "---\n" + String.join("\n", kept) + "\n---\n" + body;
  }

  private static List<String> yamlLinesWithoutImageKeys(
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

  private static String yamlLinesForImageScalars(String imageUrl, String imageMask) {
    StringBuilder sb = new StringBuilder();
    sb.append("image: ").append(formatYamlScalarValue(imageUrl.trim())).append('\n');
    if (!Strings.isBlank(imageMask)) {
      sb.append("image_mask: ").append(formatYamlScalarValue(imageMask.trim())).append('\n');
    }
    return sb.toString();
  }

  private static boolean isImageOrImageMaskPropertyLine(String line) {
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

  /** Plain scalar when safe; else double-quoted with minimal escaping. */
  static String formatYamlScalarValue(String value) {
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

  public static Optional<String> removeWikiLinksFromLeadingFrontmatterProperties(
      String content, Set<String> linkTexts) {
    if (linkTexts.isEmpty()) {
      return Optional.empty();
    }
    Optional<LeadingFrontmatter> split = splitLeadingFrontmatter(content);
    if (split.isEmpty()) {
      return Optional.empty();
    }

    List<String> keptLines = new ArrayList<>();
    boolean changed = false;
    for (String line : split.get().yamlRaw().split("\n", -1)) {
      String rewritten = line;
      for (String linkText : linkTexts) {
        rewritten = rewritten.replace("[[" + linkText + "]]", "");
      }
      if (!rewritten.equals(line)) {
        changed = true;
        if (isEmptyPropertyLine(rewritten)) {
          continue;
        }
      }
      keptLines.add(rewritten);
    }
    if (!changed) {
      return Optional.empty();
    }
    String body = split.get().body();
    if (keptLines.isEmpty()) {
      return Optional.of(body);
    }
    return Optional.of("---\n" + String.join("\n", keptLines) + "\n---\n" + body);
  }

  private static boolean isEmptyPropertyLine(String line) {
    String t = line.trim();
    int colon = t.indexOf(':');
    if (colon < 0) {
      return false;
    }
    String value = t.substring(colon + 1).trim();
    return value.isEmpty() || value.equals("\"\"") || value.equals("''");
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
