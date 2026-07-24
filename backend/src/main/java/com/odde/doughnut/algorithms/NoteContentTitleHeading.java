package com.odde.doughnut.algorithms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Doughnut stores the note title separately from markdown body. AI extract sometimes still emits
 * the title as a leading {@code #} heading; strip that redundant heading when it matches the title.
 */
public final class NoteContentTitleHeading {
  private static final Pattern LEADING_HEADING =
      Pattern.compile("\\A\\s{0,3}(#{1,6})[ \\t]+(.+?)[ \\t]*(?:\\r?\\n|\\z)");

  private NoteContentTitleHeading() {}

  public static String withoutRepeatedTitleHeading(String title, String content) {
    if (title == null || content == null || title.isBlank() || content.isBlank()) {
      return content;
    }
    Matcher matcher = LEADING_HEADING.matcher(content);
    if (!matcher.find()) {
      return content;
    }
    String headingText = matcher.group(2).strip();
    if (!headingText.equalsIgnoreCase(title.strip())) {
      return content;
    }
    String remainder = content.substring(matcher.end());
    return remainder.replaceFirst("\\A(?:\\r?\\n)+", "");
  }
}
