package com.odde.doughnut.algorithms;

import java.util.Optional;

/** Leading YAML frontmatter fence handling for note content markdown. */
public final class NoteContentMarkdown {

  private NoteContentMarkdown() {}

  public record LeadingFrontmatter(String yamlRaw, String body) {}

  public static String bodyWithoutLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content).map(LeadingFrontmatter::body).orElse(content);
  }

  /**
   * Reads a Wikidata Q-id from the first YAML frontmatter block when present. Accepts {@code
   * wikidata_id} (canonical in rich-mode property rows) or legacy {@code wikidataId}.
   */
  public static Optional<String> wikidataIdScalarFromLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content)
        .flatMap(
            lf -> {
              var fromSnake =
                  NoteYamlFrontmatterScalars.firstScalarValue(lf.yamlRaw(), "wikidata_id");
              if (fromSnake.isPresent()) {
                return fromSnake;
              }
              return NoteYamlFrontmatterScalars.firstScalarValue(lf.yamlRaw(), "wikidataId");
            });
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
