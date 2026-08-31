package com.odde.donut.algorithms;

import java.util.Optional;

/**
 * Valid frontmatter {@code note_level} (integers 1–6): parse from note markdown, or insert
 * verbatim.
 */
public final class FrontmatterNoteLevel {

  private static final String KEY = "note_level";

  private FrontmatterNoteLevel() {}

  public static Optional<Integer> fromNoteContent(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter)
        .flatMap(FrontmatterNoteLevel::fromFrontmatter);
  }

  private static Optional<Integer> fromFrontmatter(Frontmatter frontmatter) {
    return frontmatter.getString(KEY).flatMap(FrontmatterNoteLevel::parseValidLevel);
  }

  /**
   * Inserts {@code note_level: N} into the leading fence without re-dumping it, when YAML has no
   * valid key and {@code level} is 1–6. Valid YAML already present is left unchanged.
   */
  public static String withVerbatimLevel(String content, int level) {
    if (fromNoteContent(content).isPresent() || !isValidLevel(level)) {
      return content;
    }
    String line = KEY + ": " + level + "\n";
    return NoteLeadingFrontmatter.splitVerbatim(content)
        .map(split -> split.rebuild(appendLine(split.yamlRaw(), line)))
        .orElseGet(
            () -> {
              String body = content == null ? "" : content;
              return "---\n" + line + "---\n" + body;
            });
  }

  private static String appendLine(String yamlRaw, String line) {
    if (yamlRaw.isEmpty() || yamlRaw.endsWith("\n")) {
      return yamlRaw + line;
    }
    return yamlRaw + "\n" + line;
  }

  private static Optional<Integer> parseValidLevel(String raw) {
    if (raw.length() != 1) {
      return Optional.empty();
    }
    int level = raw.charAt(0) - '0';
    if (!isValidLevel(level)) {
      return Optional.empty();
    }
    return Optional.of(level);
  }

  private static boolean isValidLevel(int level) {
    return level >= 1 && level <= 6;
  }
}
