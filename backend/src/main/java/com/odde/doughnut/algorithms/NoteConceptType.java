package com.odde.doughnut.algorithms;

import java.util.Optional;
import java.util.regex.Pattern;

/** Persist-time ordinary-note concept type ({@code type: Note}) for stored markdown. */
public final class NoteConceptType {

  private static final String ORDINARY_NOTE_FENCE = "---\ntype: Note\n---\n";
  private static final Pattern TOP_LEVEL_TYPE_LINE = Pattern.compile("(?im)^type\\s*:.*(?:\\n)?");

  private NoteConceptType() {}

  public static String ensureOrdinaryNoteType(String content) {
    Optional<NoteLeadingFrontmatter.VerbatimSplit> verbatim =
        NoteLeadingFrontmatter.splitVerbatim(content);
    if (verbatim.isEmpty()) {
      String body = content == null ? "" : content;
      return ORDINARY_NOTE_FENCE + body;
    }
    NoteLeadingFrontmatter.VerbatimSplit split = verbatim.get();
    if (hasNonEmptyType(split.yamlRaw())) {
      return content;
    }
    return insertOrdinaryNoteTypeFirst(split);
  }

  private static boolean hasNonEmptyType(String yamlRaw) {
    return Frontmatter.parse(yamlRaw).getString("type").filter(s -> !s.isBlank()).isPresent();
  }

  private static String insertOrdinaryNoteTypeFirst(NoteLeadingFrontmatter.VerbatimSplit split) {
    String remainder = TOP_LEVEL_TYPE_LINE.matcher(split.yamlRaw()).replaceFirst("");
    if (!remainder.isEmpty() && !remainder.endsWith("\n")) {
      remainder = remainder + "\n";
    }
    return "---\ntype: Note\n" + remainder + "---\n" + split.body();
  }
}
