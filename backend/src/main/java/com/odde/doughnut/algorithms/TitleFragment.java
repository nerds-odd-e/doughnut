package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

record TitleFragment(boolean suffix, String stem) {
  public static TitleFragment from(String content) {
    String trimmedContent = content.trim();
    if (trimmedContent.startsWith("~")
        || trimmedContent.startsWith("〜")
        || trimmedContent.startsWith("～")) {
      return new TitleFragment(true, trimmedContent.substring(1));
    }
    return new TitleFragment(false, trimmedContent);
  }

  private ClozePatternCreator getClozePatternCreator() {
    return new ClozePatternCreator(suffix);
  }

  boolean matches(String answer) {
    return stem.equalsIgnoreCase(answer.strip());
  }

  public String replaceLiteralWords(String details, final String replacement) {
    Pattern pattern = getClozePatternCreator().getPattern(stem);
    return pattern.matcher(details).replaceAll(replacement);
  }

  public String replaceSimilar(String literal, String replacement) {
    String substring = stem.substring(0, (stem.length() + 1) * 3 / 4);
    Pattern pattern = getClozePatternCreator().getPattern(substring);
    return pattern.matcher(literal).replaceAll(replacement);
  }

  public int length() {
    return stem.length();
  }
}
