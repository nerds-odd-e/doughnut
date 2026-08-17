package com.odde.doughnut.algorithms;

import java.util.Optional;
import java.util.regex.Pattern;

/** Persist-time note concept type for stored markdown. */
public final class NoteConceptType {

  private static final String ORDINARY_TYPE = "Note";
  private static final String RELATIONSHIP_TYPE = "Relationship";
  private static final String ORDINARY_NOTE_FENCE = "---\ntype: " + ORDINARY_TYPE + "\n---\n";
  private static final Pattern TOP_LEVEL_TYPE_LINE =
      Pattern.compile("(?im)^(type\\s*:)([ \\t]*)(?:\"[^\"]*\"|'[^']*'|\\S+)?[ \\t]*$(\\n)?");

  private NoteConceptType() {}

  public static String ensureStoredType(String content) {
    Optional<NoteLeadingFrontmatter.VerbatimSplit> verbatim =
        NoteLeadingFrontmatter.splitVerbatim(content);
    if (verbatim.isEmpty()) {
      String body = content == null ? "" : content;
      return ORDINARY_NOTE_FENCE + body;
    }
    NoteLeadingFrontmatter.VerbatimSplit split = verbatim.get();
    Optional<String> type =
        Frontmatter.parse(split.yamlRaw()).getString("type").filter(s -> !s.isBlank());
    if (type.isEmpty()) {
      return insertOrdinaryNoteTypeFirst(split);
    }
    return canonicalizeTypeSpelling(content, split, type.get());
  }

  private static String canonicalizeTypeSpelling(
      String content, NoteLeadingFrontmatter.VerbatimSplit split, String type) {
    Optional<String> canonical = canonicalSpelling(type.trim());
    if (canonical.isEmpty()) {
      return content;
    }
    String newYaml =
        TOP_LEVEL_TYPE_LINE.matcher(split.yamlRaw()).replaceFirst("$1$2" + canonical.get() + "$3");
    if (newYaml.equals(split.yamlRaw())) {
      return content;
    }
    return split.rebuild(newYaml);
  }

  private static Optional<String> canonicalSpelling(String type) {
    if (ORDINARY_TYPE.equalsIgnoreCase(type)) {
      return Optional.of(ORDINARY_TYPE);
    }
    if (RELATIONSHIP_TYPE.equalsIgnoreCase(type)) {
      return Optional.of(RELATIONSHIP_TYPE);
    }
    return Optional.empty();
  }

  private static String insertOrdinaryNoteTypeFirst(NoteLeadingFrontmatter.VerbatimSplit split) {
    String remainder = TOP_LEVEL_TYPE_LINE.matcher(split.yamlRaw()).replaceFirst("");
    return split.rebuild("type: " + ORDINARY_TYPE + "\n" + remainder);
  }
}
