package com.odde.doughnut.validators;

import com.odde.doughnut.algorithms.WikiLinkTargetReference;
import java.util.regex.Pattern;

public final class DisplayNamePathSeparators {

  private DisplayNamePathSeparators() {}

  private static final Pattern SURROUNDING_WHITESPACE =
      Pattern.compile(
          "(?U)\\A[\\s\\u200B\\u200C\\u200D\\u2060\\uFEFF]+|[\\s\\u200B\\u200C\\u200D\\u2060\\uFEFF]+\\z");

  public static final String REGEXP =
      "^[\\s\\x00-\\x1F]*[^\\\\/:*?\"<>|\\x00-\\x1F]*[\\s\\x00-\\x1F]*$";

  public static final String MESSAGE =
      "Name must not contain \\ / : * ? \" < > | or ASCII control characters.";

  public static String trimSurroundingWhitespace(String value) {
    if (value == null) {
      return null;
    }
    return SURROUNDING_WHITESPACE.matcher(value).replaceAll("");
  }

  public static String normalizeDisplayName(String value) {
    return trimSurroundingWhitespace(replaceOsInvalidChars(value));
  }

  /** OS-invalid filename characters in note titles → fullwidth (ASCII controls → space). */
  public static String replaceOsInvalidChars(String value) {
    if (value == null) {
      return null;
    }
    StringBuilder converted = new StringBuilder(value.length());
    boolean changed = false;
    for (int i = 0; i < value.length(); i++) {
      char original = value.charAt(i);
      char mapped = toFullwidthOrSpace(original);
      converted.append(mapped);
      if (mapped != original) {
        changed = true;
      }
    }
    return changed ? converted.toString() : value;
  }

  private static char toFullwidthOrSpace(char c) {
    return switch (c) {
      case '\\' -> '＼';
      case '/' -> '／';
      case ':' -> '：';
      case '*' -> '＊';
      case '?' -> '？';
      case '"' -> '＂';
      case '<' -> '＜';
      case '>' -> '＞';
      case '|' -> '｜';
      default -> c <= 0x1F ? ' ' : c;
    };
  }

  /**
   * Sanitizes wiki link target tokens. Path-shaped targets ({@code Folder/Title}) keep {@code /}.
   * For {@code Notebook:Title}, only the note-title portion after the first {@code :} is converted;
   * the notebook prefix colon stays halfwidth.
   */
  public static String replaceOsInvalidCharsInWikiLinkTarget(String targetToken) {
    if (targetToken == null) {
      return null;
    }
    int colon = targetToken.indexOf(':');
    if (colon > 0 && colon < targetToken.length() - 1) {
      String notebookName = targetToken.substring(0, colon).trim();
      if (!notebookName.isEmpty() && !notebookName.contains("\\") && !notebookName.contains("/")) {
        String noteTitle = targetToken.substring(colon + 1);
        return notebookName + ":" + replaceOsInvalidChars(noteTitle);
      }
    }
    if (WikiLinkTargetReference.PathShapedTarget.tryParse(targetToken).isPresent()) {
      return targetToken.replace('\\', '＼');
    }
    return replaceOsInvalidChars(targetToken);
  }
}
