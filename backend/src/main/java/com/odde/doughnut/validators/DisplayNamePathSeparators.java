package com.odde.doughnut.validators;

import java.util.regex.Pattern;

public final class DisplayNamePathSeparators {

  private DisplayNamePathSeparators() {}

  private static final Pattern SURROUNDING_WHITESPACE =
      Pattern.compile(
          "(?U)\\A[\\s\\u200B\\u200C\\u200D\\u2060\\uFEFF]+|[\\s\\u200B\\u200C\\u200D\\u2060\\uFEFF]+\\z");

  public static final String REGEXP = "^[^\\\\/:]*$";

  public static final String MESSAGE = "Name must not contain backslash, slash, or colon.";

  public static String trimSurroundingWhitespace(String value) {
    if (value == null) {
      return null;
    }
    return SURROUNDING_WHITESPACE.matcher(value).replaceAll("");
  }

  public static String normalizeDisplayName(String value) {
    return trimSurroundingWhitespace(toFullwidthPathSeparators(value));
  }

  /** Halfwidth path separators in note titles → fullwidth (mirrors V300000174 migration). */
  public static String toFullwidthPathSeparators(String value) {
    if (value == null) {
      return null;
    }
    if (!value.contains("\\") && !value.contains("/") && !value.contains(":")) {
      return value;
    }
    // Halfwidth slash becomes escaped fullwidth ／／ so it stays literal, not a title alias.
    return value.replace('\\', '＼').replace("/", "／／").replace(':', '：');
  }

  /**
   * Sanitizes wiki link target tokens: for {@code Notebook:Note/Title}, only the note-title portion
   * after the first {@code :} is converted; the notebook prefix colon stays halfwidth.
   */
  public static String toFullwidthPathSeparatorsInWikiLinkTarget(String targetToken) {
    if (targetToken == null) {
      return null;
    }
    int colon = targetToken.indexOf(':');
    if (colon > 0 && colon < targetToken.length() - 1) {
      String notebookName = targetToken.substring(0, colon).trim();
      if (!notebookName.isEmpty() && !notebookName.contains("\\") && !notebookName.contains("/")) {
        String noteTitle = targetToken.substring(colon + 1);
        return notebookName + ":" + toFullwidthPathSeparators(noteTitle);
      }
    }
    return toFullwidthPathSeparators(targetToken);
  }
}
