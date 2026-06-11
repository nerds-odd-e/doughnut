package com.odde.doughnut.validators;

public final class DisplayNamePathSeparators {

  private DisplayNamePathSeparators() {}

  public static final String REGEXP = "^[^\\\\/:]*$";

  public static final String MESSAGE = "Name must not contain backslash, slash, or colon.";

  /** Halfwidth path separators in note titles → fullwidth (mirrors V300000174 migration). */
  public static String toFullwidthPathSeparators(String value) {
    if (value == null) {
      return null;
    }
    if (!value.contains("\\") && !value.contains("/") && !value.contains(":")) {
      return value;
    }
    return value.replace('\\', '＼').replace('/', '／').replace(':', '：');
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
