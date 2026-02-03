package com.odde.doughnut.algorithms;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HtmlOrMarkdown(String htmlOrMarkdown) {
  // Zero-width space used as a marker to indicate that the segment follows a non-whitespace char
  static final String NON_WHITESPACE_CONTEXT_MARKER = "\u200B";

  public String replaceText(Function<String, String> callback) {
    return replaceTextWithContext((text, context) -> callback.apply(text));
  }

  public String replaceTextWithContext(BiFunction<String, Boolean, String> callback) {
    Pattern pattern = Pattern.compile("(?s)(?<=^|>)[^><]+?(?=<|$)");
    Matcher matcher = pattern.matcher(htmlOrMarkdown);
    return matcher.replaceAll(
        matchResult -> {
          String text = matchResult.group();
          int start = matchResult.start();
          // Check if this segment follows a non-whitespace character (before the preceding tag)
          boolean followsNonWhitespace = checkPrecedingNonWhitespace(start);
          return Matcher.quoteReplacement(callback.apply(text, followsNonWhitespace));
        });
  }

  private boolean checkPrecedingNonWhitespace(int segmentStart) {
    if (segmentStart == 0) return false;
    // Find the character before any preceding tag
    int i = segmentStart - 1;
    // Skip backwards past the closing '>' if present
    if (i >= 0 && htmlOrMarkdown.charAt(i) == '>') {
      // Find the matching '<' of this tag
      while (i > 0 && htmlOrMarkdown.charAt(i) != '<') {
        i--;
      }
      // Now check the character before the tag
      if (i > 0) {
        char precedingChar = htmlOrMarkdown.charAt(i - 1);
        return !Character.isWhitespace(precedingChar) && precedingChar != '>';
      }
    }
    return false;
  }

  public boolean isBlank() {
    if (htmlOrMarkdown == null) return true;

    String regexBr = "(\\s*<br[^>]*/?>\\s*)";
    String regex = "^(?:\\s*((<p[^>]*>)(\\s|" + regexBr + ")*</p>\\s*)|" + regexBr + ")*$";

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(htmlOrMarkdown);

    return matcher.matches();
  }

  public String beginning(int charCount) {
    if (isBlank()) return null;
    String withoutHtmlTags = htmlOrMarkdown.replaceAll("<[^>]*>", "");
    if (withoutHtmlTags.length() <= charCount) return withoutHtmlTags;
    return withoutHtmlTags.substring(0, charCount) + "...";
  }
}
