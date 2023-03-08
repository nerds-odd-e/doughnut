package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClozePatternCreator {
  final boolean suffix;

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
    return "(?<!\\w)" + Pattern.quote(toMatch) + "(?!\\w)";
  }

  private String ignoreConjunctions(String toMatch) {
    return Arrays.stream(toMatch.split("[\\s-]+"))
        .filter(x -> !Arrays.asList("the", "a", "an").contains(x))
        .map(Pattern::quote)
        .collect(Collectors.joining("([\\s-]+)((and\\s+)|(the\\s+)|(a\\s+)|(an\\s+))?"));
  }

  private String suffixIfNeeded(String pattern) {
    if (suffix) {
      return "(?U)(?<=[^\\s])" + pattern;
    }
    return "(?<=\\b|\\p{IsHan}|\\p{IsHiragana})" + pattern;
  }

  Pattern getPattern(String toMatch) {
    return Pattern.compile(
        suffixIfNeeded(getPatternStringToMatch(toMatch)), Pattern.CASE_INSENSITIVE);
  }
}
