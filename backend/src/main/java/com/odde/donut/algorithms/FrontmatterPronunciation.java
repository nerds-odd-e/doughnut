package com.odde.donut.algorithms;

import java.util.List;

/** Frontmatter {@code pronunciation}: how the note's title is read, one value or a list. */
public final class FrontmatterPronunciation {

  private static final String KEY = "pronunciation";

  private FrontmatterPronunciation() {}

  /** The nonblank values: a scalar as one value, a list as its items. */
  public static List<String> fromNoteContent(String content) {
    return NoteContentMarkdown.leadingFrontmatter(content)
        .getPropertyValue(KEY)
        .map(FrontmatterPronunciation::values)
        .orElse(List.of())
        .stream()
        .filter(value -> !value.isBlank())
        .toList();
  }

  private static List<String> values(FrontmatterPropertyValue value) {
    return switch (value) {
      case FrontmatterPropertyValue.Scalar scalar -> List.of(scalar.value());
      case FrontmatterPropertyValue.ListItems list -> list.items();
    };
  }
}
