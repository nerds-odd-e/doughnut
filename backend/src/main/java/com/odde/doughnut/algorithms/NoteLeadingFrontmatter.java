package com.odde.doughnut.algorithms;

import java.util.Optional;

/** Leading {@code ---} fenced block at the start of note markdown: split and rebuild. */
public final class NoteLeadingFrontmatter {

  private NoteLeadingFrontmatter() {}

  public record Split(Frontmatter frontmatter, String body) {}

  /**
   * The leading block as the author wrote it — fences included, nothing reparsed or re-dumped —
   * with the YAML between the fences and the body after it. Reading a property means {@link
   * #split}; handing the block back to the author means this, because a round trip through {@link
   * Frontmatter} settles comments, key order, and quoting for them.
   */
  public record VerbatimSplit(String frontmatterBlock, String yamlRaw, String body) {}

  public static Optional<Split> split(String content) {
    return splitVerbatim(content).map(s -> new Split(Frontmatter.parse(s.yamlRaw()), s.body()));
  }

  public static Optional<VerbatimSplit> splitVerbatim(String content) {
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
        String frontmatterBlock = joinLines(lines, 0, i + 1);
        return Optional.of(new VerbatimSplit(frontmatterBlock, yamlRaw, body));
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
