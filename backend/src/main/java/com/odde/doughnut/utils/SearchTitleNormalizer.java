package com.odde.doughnut.utils;

/** Folds wave-dash title variants to ASCII tilde so literal search treats them as the same. */
public final class SearchTitleNormalizer {
  /** JPQL expression: note title with wave-dash characters folded to ASCII tilde. */
  public static final String NORMALIZED_NOTE_TITLE_JPQL =
      "REPLACE(REPLACE(n.title, '〜', '~'), '～', '~')";

  private SearchTitleNormalizer() {}

  public static String normalizeTildeVariants(String searchKey) {
    return searchKey.replace('〜', '~').replace('～', '~');
  }
}
