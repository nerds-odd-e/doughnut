package com.odde.donut.algorithms;

import java.util.Optional;

/** Reads a valid frontmatter {@code note_level} (integers 1–6) for the assimilation cache. */
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

  private static Optional<Integer> parseValidLevel(String raw) {
    if (raw.length() != 1) {
      return Optional.empty();
    }
    char digit = raw.charAt(0);
    if (digit < '1' || digit > '6') {
      return Optional.empty();
    }
    return Optional.of(digit - '0');
  }
}
