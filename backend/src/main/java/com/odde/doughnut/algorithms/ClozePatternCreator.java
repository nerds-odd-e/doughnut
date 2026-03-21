package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.regex.Pattern;

public class ClozePatternCreator {
  final boolean suffix;
  public static final String potentialWordBoundary =
      "\\b|\\p{IsHan}|\\p{IsKatakana}|\\p{IsHiragana}";

  public ClozePatternCreator(boolean suffix) {
    this.suffix = suffix;
  }

  private String getPatternStringToMatch(String toMatch) {
    if (toMatch.length() >= 4 || suffix) {
      return ignoreConjunctions(toMatch);
    }
    if (toMatch.matches("^\\d+$")) {
      return "(?<!\\d)" + Pattern.quote(toMatch) + "(?!\\d)";
    }
    return Pattern.quote(toMatch) + "(?=" + potentialWordBoundary + ")";
  }

  private String ignoreConjunctions(String toMatch) {
    String[] words =
        Arrays.stream(toMatch.split("[\\s-]+"))
            .filter(x -> !Arrays.asList("the", "a", "an").contains(x))
            .toArray(String[]::new);
    if (words.length == 0) return "";
    String between = "([\\s-]+)((and\\s+)|(the\\s+)|(a\\s+)|(an\\s+))?";
    StringBuilder sb = new StringBuilder("(?:[_*])?");
    for (int i = 0; i < words.length; i++) {
      if (i > 0) sb.append(between);
      String word = words[i];
      sb.append(Pattern.quote(word));
      if (i == words.length - 1) sb.append("(?:s|ed|ing)?(?:[_*])?(?!\\w)");
    }
    return sb.toString();
  }

  private String suffixIfNeeded(String pattern) {
    if (suffix) {
      return "(?U)(?<=[^\\s])" + pattern;
    }
    return "(?<=" + potentialWordBoundary + "|[_*])" + pattern;
  }

  Pattern getPattern(String toMatch) {
    return Pattern.compile(
        suffixIfNeeded(getPatternStringToMatch(toMatch)),
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  }
}
