package com.odde.donut.validators;

import com.odde.donut.algorithms.WikiLinkAuthoredTarget;
import com.odde.donut.algorithms.WikiLinkTargetReference;
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
   * For {@code Notebook:Title}, the notebook prefix colon stays halfwidth; both the notebook name
   * and the note-title portion are converted. An encoded {@code #prop:} suffix is left unchanged.
   */
  public static String replaceOsInvalidCharsInWikiLinkTarget(String targetToken) {
    if (targetToken == null) {
      return null;
    }
    return WikiLinkAuthoredTarget.parse(targetToken)
        .mapNoteTarget(DisplayNamePathSeparators::replaceOsInvalidCharsInNoteTarget)
        .format();
  }

  private static String replaceOsInvalidCharsInNoteTarget(String noteTarget) {
    int colon = noteTarget.indexOf(':');
    if (colon > 0 && colon < noteTarget.length() - 1) {
      String notebookName = noteTarget.substring(0, colon).trim();
      if (!notebookName.isEmpty() && !notebookName.contains("\\") && !notebookName.contains("/")) {
        String noteTitle = noteTarget.substring(colon + 1);
        return replaceOsInvalidChars(notebookName) + ":" + replaceOsInvalidChars(noteTitle);
      }
    }
    return WikiLinkTargetReference.PathShapedTarget.tryParse(noteTarget)
        .map(path -> path.mapSegmentNames(DisplayNamePathSeparators::replaceOsInvalidChars))
        .orElseGet(() -> replaceOsInvalidChars(noteTarget));
  }
}
